#!/bin/bash
# ============================================================
# Script: setup_nginx_okxonchains.sh
# Mô tả: Tự động cấu hình và bật nginx cho tên miền okxonchains.com
#        với thư mục website /var/www/cryptotrade
# Sử dụng: sudo bash setup_nginx_okxonchains.sh
# ============================================================

set -e

DOMAIN="okxonchains.com"
WEB_ROOT="/var/www/cryptotrade"
NGINX_CONF="/etc/nginx/sites-available/${DOMAIN}"
NGINX_ENABLED="/etc/nginx/sites-enabled/${DOMAIN}"

# --- Kiểm tra quyền root ---
if [ "$EUID" -ne 0 ]; then
    echo "[LỖI] Vui lòng chạy script với quyền root: sudo bash $0"
    exit 1
fi

echo "=========================================="
echo " Cấu hình nginx cho: ${DOMAIN}"
echo " Thư mục website  : ${WEB_ROOT}"
echo "=========================================="

# --- 1. Tạo thư mục web nếu chưa có ---
if [ ! -d "${WEB_ROOT}" ]; then
    echo "[+] Tạo thư mục ${WEB_ROOT} ..."
    mkdir -p "${WEB_ROOT}"
    chown -R www-data:www-data "${WEB_ROOT}"
    chmod -R 755 "${WEB_ROOT}"
    echo "[+] Đã tạo thư mục ${WEB_ROOT}"
else
    echo "[✓] Thư mục ${WEB_ROOT} đã tồn tại"
fi

# --- 2. Tạo file nginx config ---
echo "[+] Tạo file cấu hình nginx tại ${NGINX_CONF} ..."
cat > "${NGINX_CONF}" <<EOF
server {
    listen 80;
    listen [::]:80;

    server_name ${DOMAIN} www.${DOMAIN};

    root ${WEB_ROOT};
    index index.html index.htm index.php;

    # Ghi log
    access_log /var/log/nginx/${DOMAIN}.access.log;
    error_log  /var/log/nginx/${DOMAIN}.error.log;

    location / {
        try_files \$uri \$uri/ =404;
    }

    # Từ chối truy cập file ẩn (bắt đầu bằng dấu chấm)
    location ~ /\. {
        deny all;
    }

    # Gzip
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
EOF
echo "[✓] Đã tạo file cấu hình ${NGINX_CONF}"

# --- 3. Kích hoạt site (tạo symlink) ---
if [ -L "${NGINX_ENABLED}" ]; then
    echo "[✓] Symlink sites-enabled đã tồn tại, bỏ qua..."
else
    echo "[+] Kích hoạt site ${DOMAIN} ..."
    ln -s "${NGINX_CONF}" "${NGINX_ENABLED}"
    echo "[✓] Đã tạo symlink ${NGINX_ENABLED}"
fi

# --- 4. Tắt site mặc định nếu còn tồn tại (tuỳ chọn) ---
if [ -L "/etc/nginx/sites-enabled/default" ]; then
    echo "[+] Vô hiệu hoá site mặc định (default)..."
    rm -f "/etc/nginx/sites-enabled/default"
    echo "[✓] Đã xoá symlink default"
fi

# --- 5. Kiểm tra cú pháp nginx ---
echo "[+] Kiểm tra cú pháp cấu hình nginx..."
if nginx -t; then
    echo "[✓] Cú pháp nginx hợp lệ"
else
    echo "[LỖI] Cú pháp nginx không hợp lệ. Kiểm tra lại file ${NGINX_CONF}"
    exit 1
fi

# --- 6. Khởi động lại nginx ---
echo "[+] Khởi động lại nginx..."
if systemctl is-active --quiet nginx; then
    systemctl reload nginx
    echo "[✓] nginx đã được reload thành công"
else
    systemctl start nginx
    echo "[✓] nginx đã được khởi động"
fi

# --- 7. Bật nginx khởi động cùng hệ thống ---
systemctl enable nginx &>/dev/null
echo "[✓] nginx đã được bật tự khởi động cùng hệ thống"

echo ""
echo "=========================================="
echo " HOÀN THÀNH!"
echo " Tên miền : http://${DOMAIN}"
echo " Thư mục  : ${WEB_ROOT}"
echo ""
echo " Gợi ý: Cài SSL miễn phí với Certbot:"
echo "   sudo apt install certbot python3-certbot-nginx -y"
echo "   sudo certbot --nginx -d ${DOMAIN} -d www.${DOMAIN}"
echo "=========================================="
