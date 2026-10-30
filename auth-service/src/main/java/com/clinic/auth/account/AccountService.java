package com.clinic.auth.account;

import com.clinic.auth.account.dto.AccountResponse;
import com.clinic.auth.account.dto.CreateAccountRequest;
import com.clinic.auth.account.dto.RegisterRequest;
import com.clinic.auth.common.BusinessException;
import com.clinic.auth.keycloak.KeycloakAdminClient;
import com.clinic.auth.keycloak.KeycloakUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

// Bảng accounts và Keycloak là hai nơi lưu, không chung một giao dịch được. Vì vậy các hàm ghi ở đây
// cố ý không có @Transactional: làm từng bước, bước sau lỗi thì xóa / hoàn tác bước trước (bù trừ)
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final KeycloakAdminClient keycloak;

    public AccountService(AccountRepository accountRepository, KeycloakAdminClient keycloak) {
        this.accountRepository = accountRepository;
        this.keycloak = keycloak;
    }

    public AccountResponse register(RegisterRequest request) {
        return createInBothPlaces(request.username(), request.password(), request.fullName(), request.email(),
            request.phone(), Role.PATIENT, null);
    }

    public AccountResponse getById(Long id) {
        return AccountResponse.from(find(id));
    }

    public List<AccountResponse> list(Role role, String callerRole) {
        requireAdmin(callerRole);
        List<Account> accounts = role == null
            ? accountRepository.findAll(Sort.by("id"))
            : accountRepository.findByRoleOrderById(role);
        return accounts.stream().map(AccountResponse::from).toList();
    }

    public AccountResponse create(CreateAccountRequest request, String callerRole) {
        requireAdmin(callerRole);
        Long doctorId = null;
        if (request.role() == Role.DOCTOR) {
            // CS33: không gọi Provider kiểm doctorId; trang Tài khoản cho chọn bác sĩ từ danh sách có sẵn
            if (request.doctorId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "DOCTOR_ID_REQUIRED",
                    "Tài khoản bác sĩ phải gắn với một bác sĩ (doctorId)");
            }
            doctorId = request.doctorId();
        }
        return createInBothPlaces(request.username(), request.password(), request.fullName(), request.email(),
            request.phone(), request.role(), doctorId);
    }

    public AccountResponse lock(Long id, Long callerId, String callerRole) {
        requireAdmin(callerRole);
        if (id.equals(callerId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CANNOT_LOCK_SELF", "Không thể tự khóa tài khoản đang dùng");
        }
        return changeEnabled(id, false);
    }

    public AccountResponse unlock(Long id, String callerRole) {
        requireAdmin(callerRole);
        return changeEnabled(id, true);
    }

    private AccountResponse createInBothPlaces(String rawUsername, String password, String fullName, String rawEmail,
                                               String phone, Role role, Long doctorId) {
        // Keycloak tự đổi tên đăng nhập về chữ thường; đổi trước để hai bên giống nhau
        String username = rawUsername.trim().toLowerCase(Locale.ROOT);
        String email = rawEmail == null || rawEmail.isBlank() ? null : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByUsername(username)) {
            throw new BusinessException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Tên đăng nhập đã có người dùng");
        }
        if (email != null && accountRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email đã được dùng cho tài khoản khác");
        }

        // Bước 1: ghi accounts trước để có accountId (số) đưa vào token
        Account account = new Account();
        account.setUsername(username);
        account.setFullName(fullName.trim());
        account.setEmail(email);
        account.setPhone(phone == null || phone.isBlank() ? null : phone);
        account.setRole(role);
        account.setDoctorId(doctorId);
        try {
            account = accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Tên đăng nhập hoặc email đã có người dùng");
        }

        // Bước 2: tạo người dùng Keycloak; lỗi thì xóa dòng vừa ghi để không còn tài khoản không đăng nhập được
        String keycloakUserId;
        try {
            keycloakUserId = keycloak.createUser(new KeycloakUser(username, password, account.getFullName(), email,
                account.getId(), role.name(), doctorId));
        } catch (RuntimeException ex) {
            accountRepository.deleteById(account.getId());
            throw ex;
        }

        // Bước 3: gán vai trò, ghi liên kết; lỗi thì xóa cả hai bên
        try {
            keycloak.assignRealmRole(keycloakUserId, role.name());
            account.setKeycloakUserId(keycloakUserId);
            return AccountResponse.from(accountRepository.save(account));
        } catch (RuntimeException ex) {
            keycloak.deleteUserQuietly(keycloakUserId);
            accountRepository.deleteById(account.getId());
            throw ex;
        }
    }

    private AccountResponse changeEnabled(Long id, boolean enabled) {
        Account account = find(id);
        if (account.getKeycloakUserId() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "NOT_LINKED_TO_KEYCLOAK",
                "Tài khoản chưa liên kết người dùng Keycloak");
        }
        keycloak.setEnabled(account.getKeycloakUserId(), enabled);
        account.setEnabled(enabled);
        try {
            return AccountResponse.from(accountRepository.save(account));
        } catch (RuntimeException ex) {
            try {
                keycloak.setEnabled(account.getKeycloakUserId(), !enabled);
            } catch (RuntimeException undoEx) {
                log.error("Bù trừ thất bại: tài khoản {} lệch trạng thái khóa giữa accounts và Keycloak", id, undoEx);
            }
            throw ex;
        }
    }

    private Account find(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Tài khoản", id));
    }

    private static void requireAdmin(String callerRole) {
        if (!Role.ADMIN.name().equals(callerRole)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Chỉ quản trị viên được làm việc này");
        }
    }
}
