import sys
from pathlib import Path

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle

sys.stdout.reconfigure(encoding='utf-8')
OUT = Path(__file__).resolve().parent
W, H, DPI = 1600, 1090, 100
plt.rcParams['font.family'] = ['Segoe UI', 'Arial', 'DejaVu Sans']
plt.rcParams['svg.fonttype'] = 'none'

C = {
    'client': ('#9ca3af', '#374151'),
    'petclinic': ('#2563eb', '#1e40af'),
    'team': ('#ea580c', '#9a3412'),
    'ext': ('#16a34a', '#166534'),
    'infra': ('#ca8a04', '#854d0e'),
}
BG, BORDER, INK, GREY = '#f5f5f5', '#e5e7eb', '#111827', '#6b7280'

fig = plt.figure(figsize=(W / DPI, H / DPI), dpi=DPI)
ax = fig.add_axes([0, 0, 1, 1])
ax.set_xlim(0, W)
ax.set_ylim(H, 0)
ax.axis('off')
fig.patch.set_facecolor(BG)
checked = []  # (text, khung chứa) để tự kiểm chữ đè nhau / tràn khung


def pt(px):
    return px * 72 / DPI


def text(x, y, s, size, color=INK, weight='normal', ha='center', va='center', bg=None, frame=None, z=5):
    t = ax.text(x, y, s, fontsize=pt(size), color=color, weight=weight, ha=ha, va=va, zorder=z,
                bbox=dict(boxstyle='square,pad=0.15', fc=bg, ec='none') if bg else None)
    checked.append((t, frame))
    return t


def rect(x, y, w, h, fc='#ffffff', ec=BORDER, lw=1.5, ls='-', z=2):
    ax.add_patch(Rectangle((x, y), w, h, fc=fc, ec=ec, lw=lw, ls=ls, zorder=z))


def box(x, y, w, h, lines, kind, size=15, dashed=False):
    bar, ink = C[kind]
    rect(x, y, w, h, ec=ink if dashed else BORDER, lw=2 if dashed else 1.5, ls=(0, (5, 3)) if dashed else '-')
    if not dashed:
        rect(x, y, 6, h, fc=bar, ec=bar, lw=0, z=3)
    lh = size * 1.38
    y0 = y + h / 2 - lh * (len(lines) - 1) / 2
    for k, s in enumerate(lines):
        text(x + 3 + w / 2, y0 + k * lh, s, size if k == 0 else size - 2, ink if k < 2 else GREY,
             'bold' if k == 0 else 'normal', frame=(x, y, w, h))


def line(pts, label='', lpos=None, dashed=False, arrow=True, z=1):
    xs, ys = zip(*pts)
    ls = (0, (6, 4)) if dashed else '-'
    ax.plot(xs, ys, color='#4b5563', lw=1.6, ls=ls, zorder=z, solid_capstyle='butt')
    if arrow:
        ax.annotate('', xy=pts[-1], xytext=pts[-2], zorder=z,
                    arrowprops=dict(arrowstyle='-|>', color='#4b5563', lw=0, mutation_scale=16, shrinkA=0, shrinkB=0))
    if label:
        text(lpos[0], lpos[1], label, 13, '#374151', bg=BG, z=6)


def band(x, y, w, h, title, items, kind):
    rect(x, y, w, h, lw=1.5)
    if title:
        text(x + 14, y + 22, title, 18, weight='bold', ha='left')
    for s, cx in items:
        rect(cx - 9, y + h / 2 + (10 if title else 0) - 6, 12, 12, fc=C[kind][0], ec=C[kind][0], lw=0, z=3)
        text(cx + 9, y + h / 2 + (10 if title else 0), s, 15, ha='left', frame=(x, y, w, h))


text(W / 2, 30, 'Kiến trúc tổng thể — Nền tảng đặt lịch khám đa khoa', 26, weight='bold')

# C1 — người dùng theo vai trò
band(30, 58, 1540, 78, 'Clients',
     [('Bệnh nhân / người nhà', 70), ('Lễ tân', 360), ('Bác sĩ', 545), ('Quản trị phòng khám', 735),
      ('Khách chưa đăng nhập', 1010)], 'client')
text(1555, 75, 'vai trò: PATIENT · RECEPTIONIST · DOCTOR · ADMIN', 12, GREY, ha='right')

# C2 — web, N1 — Keycloak (bên trái, ngay trên Auth), N2 — Eureka (góc phải)
box(530, 170, 540, 66, ['Web Angular :4200', 'trang theo vai trò · mỗi phần P1–P5 làm trang của mình'], 'team')
box(30, 162, 240, 98, ['Keycloak :8180', 'tài khoản · vai trò · cấp JWT', 'P4'], 'ext')
box(1330, 162, 240, 98, ['Eureka :8761', 'danh bạ service (Discovery)', 'mọi service tự đăng ký tên'], 'petclinic')

# C3 — API Gateway: khung Petclinic + phần nhóm viết
GX, GY, GW, GH = 270, 284, 1030, 108
rect(GX, GY, GW, GH, ec=C['petclinic'][0], lw=2.5)
text(GX + 14, GY + 22, 'API Gateway :8080', 19, C['petclinic'][1], 'bold', ha='left')
text(GX + 210, GY + 23, 'cửa vào duy nhất · đặt trên khung Petclinic', 13, GREY, ha='left')
box(GX + 10, GY + 42, 330, 56, ['Định tuyến + Circuit Breaker', 'ngắt riêng từng route · Fallback · Eureka'], 'petclinic', 14)
box(GX + 350, GY + 42, 330, 56, ['Xác minh JWT + vai trò (P4)', 'gắn X-User-* · X-Correlation-Id'], 'team', 14)
box(GX + 690, GY + 42, 330, 56, ['API Composition (P4)', '/composite: ghép Scheduling + Provider + Patient'], 'team', 14)

# S — 5 service nghiệp vụ, cùng cột với database của mình ở dải dữ liệu
SW, SY, SH = 250, 600, 92
SX = [85, 380, 675, 970, 1265]
svc = [
    (['Auth Service :8081', 'đăng ký · quản lý tài khoản', 'cấp accountId · P4'], 'auth_db', False),
    (['Provider Service :8082', 'chuyên khoa · bác sĩ · phòng · ca', 'P3'], 'provider_db', False),
    (['Scheduling Service :8084', 'tìm slot · giữ chỗ · chống trùng', 'P2'], 'scheduling_db', False),
    (['Patient Service (MPI) :8083', 'hồ sơ · nghi trùng · gộp / tách', 'P1'], 'patient_db', False),
    (['Reminder Service :8085', 'cron 5 phút · gửi email nhắc', 'P5'], 'reminder_db', False),
]
for x, (lab, _, dashed) in zip(SX, svc):
    box(x, SY, SW, SH, lab, 'team', 15, dashed)

# N — dịch vụ nền, đặt giữa thanh nhánh của Gateway và hàng service, tránh các nhánh xuống
NY, NH = 470, 70
box(815, NY, 265, NH, ['RabbitMQ :5672', 'patient.events · hàng đợi + DLQ'], 'ext', 14)
box(1110, NY, 265, NH, ['Email — Mailpit (SMTP :1025)', 'hộp thư giả lập, xem thư :8025'], 'ext', 14)

# D — một MySQL, 5 database
DY, DH = 806, 96
rect(30, DY, 1540, DH, lw=1.5)
rect(30, DY, 6, DH, fc=C['ext'][0], ec=C['ext'][0], lw=0, z=3)
text(48, DY + 76, 'MySQL 8.4', 16, C['ext'][1], 'bold', ha='left')
text(150, DY + 76, 'một instance (cổng máy :3307) · 6 database: 5 của service + keycloak_db · mỗi tài khoản chỉ có quyền trên database của mình',
     13, GREY, ha='left')
# (tên, x, rộng, x của đường nối); x đường nối chọn để không cắt qua khối và nhãn khác
DB = [('keycloak_db', 36, 114, 45), ('auth_db', 160, 100, 210), ('provider_db', 390, 140, 460),
      ('scheduling_db', 670, 140, 740), ('patient_db', 1050, 140, 1120), ('reminder_db', 1290, 140, 1360)]
for name, x, w, _ in DB:
    rect(x, DY + 12, w, 44, lw=1.4, z=4)
    text(x + w / 2, DY + 34, name, 14, C['ext'][1], 'bold', frame=(x, DY + 12, w, 44))

# H — hạ tầng
band(30, 926, 1540, 56, '',
     [('Docker Compose: 12 container (thêm Keycloak)', 60),
      ('Mạng nội bộ của Compose: gọi nhau bằng tên container', 560),
      ('Cổng mở ra máy: 4200 · 8080 · 8180 · 8761 · 15672 · 8025 · 3307', 1060)], 'infra')

# Đường nối (mã L ở đặc tả)
line([(800, 136), (800, 170)], 'trình duyệt', (800, 153))                                   # L1
line([(530, 203), (270, 203)], 'đăng nhập → nhận JWT', (400, 203))                       # L2
line([(800, 236), (800, 284)], 'REST /api/v1/** + JWT', (800, 260))                          # L3
line([(270, 330), (200, 330), (200, 260)], 'khóa công khai', (200, 300))                # L4
line([(800, 392), (800, 420)], arrow=False)
line([(210, 420), (1390, 420)], arrow=False)
line([(210, 420), (210, SY)], '/auth · /accounts', (210, 446))                 # L5
line([(505, 420), (505, SY)], 'bác sĩ · phòng · ca', (505, 446))                             # L6
line([(800, 420), (800, SY)], 'slot · lịch hẹn', (800, 446))                                 # L7
line([(1095, 420), (1095, SY)], 'hồ sơ · hàng chờ trùng', (1095, 446))                       # L8
line([(1390, 420), (1390, SY)], 'nhật ký nhắc', (1390, 446))                                 # L9
line([(715, 692), (715, 728), (540, 728), (540, 692)], 'giờ làm việc theo ngày', (628, 728))  # L10
line([(1320, 692), (1320, 770), (895, 770), (895, 692)], 'lịch CONFIRMED 24 giờ tới', (1220, 770))  # L11
line([(1290, 692), (1290, 728), (1180, 728), (1180, 692)], 'email của hồ sơ', (1235, 728))   # L12
line([(1010, SY), (1010, NY + NH)], 'gộp / tách hồ sơ', (1010, 570), dashed=True)       # L13
line([(880, NY + NH), (880, SY)], 'chuyển lịch hẹn', (880, 570), dashed=True)                    # L14
line([(1300, SY), (1300, NY + NH)], 'email nhắc lịch', (1300, 570))                              # L15
line([(100, SY), (100, 260)], 'tạo tài khoản\n(Admin API)', (100, 400))                   # L18
for _, _, _, lx in DB[1:]:
    line([(lx, SY + SH), (lx, DY + 12)], z=3)                                                   # L17
line([(45, 260), (45, DY + 12)], z=3)                                                             # L19

# Chú thích
ly, lx = 1004, 40
for kind, s in [('client', 'Cửa vào (Clients)'), ('petclinic', 'Có sẵn từ khung Petclinic'), ('team', 'Nhóm tự viết'),
                ('ext', 'Phần mềm có sẵn, mượn ngoài'), ('infra', 'Hạ tầng')]:
    rect(lx, ly - 8, 16, 16, fc=C[kind][0], ec=C[kind][0], lw=0)
    text(lx + 24, ly, s, 15, ha='left')
    lx += 24 + len(s) * 8 + 34
ax.plot([lx, lx + 40], [ly, ly], color='#4b5563', lw=1.6)
text(lx + 48, ly, 'gọi HTTP đồng bộ', 15, ha='left')
ax.plot([lx + 200, lx + 240], [ly, ly], color='#4b5563', lw=1.6, ls=(0, (6, 4)))
text(lx + 248, ly, 'bất đồng bộ qua RabbitMQ', 15, ha='left')
text(40, 1040, 'Mũi tên: bên gọi → bên được gọi; nhãn: dữ liệu đi qua. '
     'Không vẽ đường service → Eureka (mọi service đều đăng ký tên với Eureka).', 13, GREY, ha='left')
text(40, 1064, 'Đặc tả, mã khối, kịch bản đi thử: docs/so-do/so-do-tong-quan-dac-ta.md', 13, GREY, ha='left')

# Tự kiểm: chữ không đè nhau, không tràn khối chứa, không tràn khổ hình
fig.canvas.draw()
r = fig.canvas.get_renderer()
inv = ax.transData.inverted()
bbs = []
for t, frame in checked:
    (x0, y1), (x1, y0) = inv.transform(t.get_window_extent(r).get_points())
    bbs.append((t.get_text(), x0, y0, x1, y1))
    if frame and (x0 < frame[0] + 6 or x1 > frame[0] + frame[2] - 2):
        print('TRÀN KHỐI:', t.get_text())
    if x0 < 0 or x1 > W or y0 < 0 or y1 > H:
        print('TRÀN KHỔ:', t.get_text())
for i in range(len(bbs)):
    for j in range(i + 1, len(bbs)):
        a, b = bbs[i], bbs[j]
        if a[1] < b[3] and b[1] < a[3] and a[2] < b[4] and b[2] < a[4]:
            print('ĐÈ NHAU:', a[0], '|', b[0])

fig.savefig(OUT / 'so-do-tong-quan.png', dpi=DPI, facecolor=BG)
fig.savefig(OUT / 'so-do-tong-quan.svg', facecolor=BG)
print('ok')
