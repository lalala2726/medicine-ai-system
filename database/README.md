# 数据库初始化说明

本目录保存项目本地启动需要的数据库初始化文件。

## 目录说明

- [MySQL](/Users/zhangchuang/IdeaProjects/medicine_all/medicine_all_open_source/database/MySQL)：MySQL 业务库初始化脚本。
- [MongoDB](/Users/zhangchuang/IdeaProjects/medicine_all/medicine_all_open_source/database/MongoDB)：MongoDB 地区集合数据和导入脚本。

## MySQL 导入

先进入 MySQL 初始化目录，创建数据库，再导入 `medicine.sql`：

```bash
cd MySQL
mysql -h localhost -P 3306 -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS medicine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -h localhost -P 3306 -u root -p123456 medicine < medicine.sql
```

如果你的 MySQL 密码不是 `123456`，把命令里的 `-p123456` 改成自己的密码。

## MongoDB 地区数据导入

先进入 MongoDB 初始化目录：

```bash
cd MongoDB
```

`import_regions.py` 默认导入到：

```text
mongodb://root:123456@localhost:27017/medicine?authSource=admin
```

在当前目录执行：

```bash
python3 import_regions.py
```

如果本机没有 `pymongo`：

```bash
python3 -m pip install pymongo
python3 import_regions.py
```

如果目标 MongoDB 连接信息不同：

```bash
TARGET_HOST=localhost \
TARGET_PORT=27017 \
TARGET_USERNAME=root \
TARGET_PASSWORD=123456 \
TARGET_AUTH_DB=admin \
TARGET_DB=medicine \
TARGET_COLLECTION=regions \
python3 import_regions.py
```

导入脚本会先删除目标 `regions` 集合，再写入 `regions.jsonl.gz` 中的数据。

## 校验

MySQL 导入后可执行：

```bash
cd ../MySQL
mysql -h localhost -P 3306 -u root -p123456 -D medicine -e "SHOW TABLES;"
```

MongoDB 导入后可执行：

```bash
cd ../MongoDB
python3 - <<'PY'
from pymongo import MongoClient

client = MongoClient("mongodb://root:123456@localhost:27017/medicine?authSource=admin")
print(client["medicine"]["regions"].count_documents({}))
client.close()
PY
```
