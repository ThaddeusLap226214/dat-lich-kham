import os

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyArrowPatch, Rectangle

OUT = os.path.dirname(os.path.abspath(__file__))
W, H = 1600, 1000
plt.rcParams["font.family"] = ["Segoe UI", "Arial", "DejaVu Sans"]

C = {
    "client": ("#9ca3af", "#374151"),
    "petclinic": ("#2563eb", "#1e40af"),
    "own": ("#ea580c", "#9a3412"),
    "ext": ("#16a34a", "#166534"),
}
GREY, INTERNAL, EVENT, BG = "#4b5563", "#7c3aed", "#16a34a", "#f5f5f5"

fig = plt.figure(figsize=(W / 100, H / 100), dpi=100)
ax = fig.add_axes([0, 0, 1, 1])
ax.set_xlim(0, W)
ax.set_ylim(H, 0)
ax.axis("off")
fig.patch.set_facecolor(BG)


def pt(px):
    # matplotlib đo chữ theo point; ảnh lưu ở dpi 100 nên 1 px = 0,72 pt
    return px * 0.72


def box(x, y, w, h, lines, kind, size=15, pending=False):
    stripe, text = C[kind]
    if pending:
        ax.add_patch(Rectangle((x, y), w, h, facecolor="white", edgecolor=stripe, lw=2, ls=(0, (6, 4)), zorder=3))
    else:
        ax.add_patch(Rectangle((x, y), w, h, facecolor="white", edgecolor="#e5e7eb", lw=1.5, zorder=3))
        ax.add_patch(Rectangle((x, y), 6, h, facecolor=stripe, edgecolor="none", zorder=4))
    lh = size * 1.45
    y0 = y + h / 2 - lh * (len(lines) - 1) / 2
    for k, s in enumerate(lines):
        first = k == 0
        ax.text(x + 3 + w / 2, y0 + k * lh, s, ha="center", va="center", zorder=5,
                fontsize=pt(size + 2 if first else size - 2), fontweight="bold" if first else "normal",
                color=text if first else ("#6b7280" if k == len(lines) - 1 and len(lines) > 2 else text))


def label(x, y, s, color="#374151", size=12.5, ha="center", bold=False):
    ax.text(x, y, s, ha=ha, va="center", fontsize=pt(size), color=color, zorder=7,
            fontweight="bold" if bold else "normal",
            bbox=dict(facecolor=BG, edgecolor="none", pad=1.5))


def arrow(pts, color=GREY, dashed=False, lw=1.6, head=True):
    ls = (0, (6, 4)) if dashed else "-"
    xs, ys = zip(*pts)
    if not head:
        ax.plot(xs, ys, color=color, lw=lw, ls=ls, zorder=2, solid_capstyle="butt")
        return
    if len(pts) > 2:
        ax.plot(xs[:-1], ys[:-1], color=color, lw=lw, ls=ls, zorder=2, solid_capstyle="butt")
    ax.add_patch(FancyArrowPatch(pts[-2], pts[-1], arrowstyle="-|>", mutation_scale=14, color=color,
                                 lw=lw, ls=ls, shrinkA=0, shrinkB=0, zorder=2))


ax.text(W / 2, 28, "Sơ đồ kết nối và quy tắc — Nền tảng đặt lịch khám đa khoa", ha="center", va="center",
        fontsize=pt(26), fontweight="bold", color="#111827")
ax.text(W / 2, 58, "Nhãn R… trên mũi tên là mã quy tắc trong docs/07-quy-tac-ket-noi.md", ha="center",
        va="center", fontsize=pt(14), color="#6b7280")

# Hàng 1: web và Keycloak
box(540, 80, 520, 70, ["Web Angular :4200", "5 vai trò: Khách · Bệnh nhân · Lễ tân · Bác sĩ · Quản trị"], "client")
box(1290, 80, 274, 100, ["Keycloak :8180", "tài khoản · vai trò · cấp JWT", "lưu ở keycloak_db (R14)"], "ext")
arrow([(1060, 115), (1290, 115)], dashed=True)
label(1175, 115, "R3 chỉ để đăng nhập")

# Hàng 2: Gateway và Eureka
box(420, 210, 760, 100, ["API Gateway :8080",
                         "chỉ có route /api/v1/** · xác minh JWT + vai trò · gắn X-User-* · Circuit Breaker từng route",
                         "API Composition /api/v1/composite/** gọi /internal của 3 service"], "petclinic")
box(44, 215, 290, 90, ["Discovery Server (Eureka) :8761", "mọi service tự đăng ký tên", "danh bạ để gọi theo tên"],
    "petclinic", size=14)
arrow([(800, 150), (800, 210)])
label(800, 180, "R1 R2 · /api/v1/** + Bearer JWT")
arrow([(1180, 260), (1427, 260), (1427, 180)])
label(1300, 260, "R4 R5 khóa công khai (JWKS)")
arrow([(420, 260), (334, 260)], dashed=True)
label(377, 260, "R10 tra tên")

# Thanh định tuyến của Gateway
CX = [176, 490, 804, 1118, 1432]
BUS = 352
arrow([(800, 310), (800, BUS)], head=False)
arrow([(CX[0], BUS), (CX[-1], BUS)], head=False)
for cx in CX:
    arrow([(cx, BUS), (cx, 480)])
label(512, 334, "R6 R7 R8 R9 · lb://<service>/api/v1/** + X-User-Id · X-User-Role · X-Doctor-Id · X-Correlation-Id",
      size=12.5, ha="center")
label(1300, 334, "Không có route /internal/** → gọi từ ngoài nhận 404 (R6)", color="#dc2626", size=12.5, bold=True)

# Gọi nội bộ /internal/v1: đi thẳng giữa các service, không qua Gateway
arrow([(730, 480), (730, 425), (560, 425), (560, 480)], color=INTERNAL, lw=2)
label(645, 425, "R10–R12 giờ làm việc", color=INTERNAL)
arrow([(1370, 480), (1370, 425), (1190, 425), (1190, 480)], color=INTERNAL, lw=2)
label(1280, 425, "R10–R12 email hồ sơ", color=INTERNAL)
# Chừa khe trên đường xám chỗ giao nhau để thấy đường tím đi ngang qua, không rẽ nhánh
ax.add_patch(Rectangle((CX[3] - 3, 381), 6, 14, facecolor=BG, edgecolor="none", zorder=2.5))
arrow([(1400, 480), (1400, 388), (870, 388), (870, 480)], color=INTERNAL, lw=2)
label(995, 388, "R10–R12 lịch CONFIRMED 24 giờ tới", color=INTERNAL)
ax.text(196, 400, "Gọi nội bộ /internal/v1:", fontsize=pt(13), color=INTERNAL, fontweight="bold", va="center")
ax.text(196, 420, "service → service, không qua Gateway", fontsize=pt(13), color=INTERNAL, va="center")

# Hàng 3: các service
SW = 264
box(CX[0] - SW / 2, 480, SW, 90, ["Auth Service :8081", "đăng ký · quản lý tài khoản", "cấp accountId · P4"], "own", size=15)
box(CX[1] - SW / 2, 480, SW, 90, ["Provider Service :8082", "chuyên khoa · bác sĩ · phòng · ca", "P3"], "own", size=15)
box(CX[2] - SW / 2, 480, SW, 90, ["Scheduling Service :8084", "slot · giữ chỗ · lịch hẹn", "P2"], "own", size=15)
box(CX[3] - SW / 2, 480, SW, 90, ["Patient Service (MPI) :8083", "hồ sơ · nghi trùng · gộp / tách", "P1"], "own",
    size=15)
box(CX[4] - SW / 2, 480, SW, 90, ["Reminder Service :8085", "cron 5 phút · gửi email nhắc", "P5"], "own", size=15)

# Hàng 4: RabbitMQ và Email
box(811, 625, 300, 80, ["RabbitMQ :5672", "exchange patient.events (topic)",
                        "→ scheduling.patient-events · lỗi → .dlq"], "ext", size=14)
arrow([(1060, 570), (1060, 625)], color=EVENT, dashed=True, lw=2)
label(1060, 597, "R15 phát", color=EVENT)
arrow([(860, 625), (860, 570)], color=EVENT, dashed=True, lw=2)
label(860, 597, "R16 nhận", color=EVENT)
box(1380, 625, 184, 80, ["Email — Mailpit", "SMTP mailpit:1025", "xem thư ở :8025"], "ext", size=14)
arrow([(1470, 570), (1470, 625)])
label(1470, 597, "R17 SMTP")

# MySQL: mỗi service một database; chữ đặt dưới các ô để đường R13 không cắt qua chữ
ax.add_patch(Rectangle((30, 752), 1540, 120, facecolor="white", edgecolor="#e5e7eb", lw=1.5, zorder=3))
ax.add_patch(Rectangle((30, 752), 6, 120, facecolor=C["ext"][0], edgecolor="none", zorder=4))
ax.text(48, 850, "MySQL 8.4", fontsize=pt(18), fontweight="bold", color=C["ext"][1], va="center", zorder=5)
ax.text(170, 851, "một instance (cổng máy 3307) · mỗi service một database + một tài khoản chỉ có quyền trên database đó",
        fontsize=pt(14), color="#6b7280", va="center", zorder=5)
DB = [(176, "auth_db", "tk auth_user", False), (490, "provider_db", "tk provider_user", False),
      (720, "scheduling_db", "tk scheduling_user", False), (1180, "patient_db", "tk patient_user", False),
      (1340, "reminder_db", "tk reminder_user", False), (1492, "keycloak_db", "tk keycloak_user", False)]
for x, name, sub, pend in DB:
    w = 136 if name == "keycloak_db" else 150
    ax.add_patch(Rectangle((x - w / 2, 765), w, 54, facecolor="white", edgecolor=C["ext"][0], lw=1.6,
                           ls=(0, (5, 3)) if pend else "-", zorder=4))
    ax.text(x, 783, name, ha="center", va="center", fontsize=pt(15), fontweight="bold", color=C["ext"][1], zorder=5)
    ax.text(x, 804, sub, ha="center", va="center", fontsize=pt(12.5), color="#6b7280", zorder=5)
for x in (176, 490, 720, 1180, 1340):
    arrow([(x, 570), (x, 765)])
    label(x, 730, "R13")
arrow([(1564, 125), (1586, 125), (1586, 792), (1560, 792)], dashed=True)
label(1586, 730, "R14", size=12)
arrow([(44, 525), (20, 525), (20, 195), (1350, 195), (1350, 180)], dashed=True)
label(220, 195, "R23 tạo tài khoản (Admin API)")

# Chú thích
ly = 905
items = [("client", "Cửa vào"), ("petclinic", "Có sẵn từ Petclinic"), ("own", "Nhóm tự viết"),
         ("ext", "Mượn ngoài")]
lx = 40
for kind, t in items:
    ax.add_patch(Rectangle((lx, ly - 8), 16, 16, facecolor=C[kind][0], edgecolor="none"))
    ax.text(lx + 24, ly, t, fontsize=pt(14), color="#111827", va="center")
    lx += 24 + len(t) * 7.2 + 28
for color, dashed, t in [(GREY, False, "HTTP qua Gateway"), (INTERNAL, False, "HTTP nội bộ /internal"),
                         (EVENT, True, "sự kiện qua RabbitMQ"), (GREY, True, "đăng nhập, tra tên, Admin API")]:
    ax.plot([lx, lx + 36], [ly, ly], color=color, lw=2, ls=(0, (6, 4)) if dashed else "-")
    ax.text(lx + 44, ly, t, fontsize=pt(14), color="#111827", va="center")
    lx += 44 + len(t) * 7.0 + 26
ax.text(40, 945, "P4 chốt 19/10/2026: Keycloak, Auth Service thu hẹp, keycloak_db trong MySQL chung. Lỗi theo ApiError (R18–R19); "
                 "Git và phạm vi sửa (R20–R22) không vẽ — xem bảng trong docs/07.",
        fontsize=pt(13), color="#6b7280", va="center")
ax.text(40, 970, "Mọi service đều đăng ký với Eureka (không vẽ để đỡ rối). Keycloak chưa có trong docker-compose.yml (P4 sẽ thêm).", fontsize=pt(13), color="#6b7280", va="center")

fig.savefig(os.path.join(OUT, "so-do-ket-noi.png"), dpi=100, facecolor=BG)
fig.savefig(os.path.join(OUT, "so-do-ket-noi.svg"), facecolor=BG)
print("ok")
