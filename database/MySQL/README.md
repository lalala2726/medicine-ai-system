# MySQL 初始化说明

本目录保存 MySQL 业务库初始化脚本。

## 文件说明

- `medicine.sql`：MySQL 业务库初始化脚本，对应数据库名 `medicine`。

## 导入命令

```bash
mysql -h localhost -P 3306 -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS medicine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -h localhost -P 3306 -u root -p123456 medicine < medicine.sql
```

如果你的 MySQL 密码不是 `123456`，把命令里的 `-p123456` 改成自己的密码。

## 导入后校验

```bash
mysql -h localhost -P 3306 -u root -p123456 -D medicine -e "SHOW TABLES;"
```
