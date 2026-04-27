# MongoDB 初始化说明

本目录保存 MongoDB 地区集合数据和导入脚本。

## 文件说明

- `regions.jsonl.gz`：MongoDB 地区集合数据，对应 `medicine.regions`。
- `regions.indexes.json`：MongoDB 源集合索引信息备份。
- `regions.metadata.json`：MongoDB 地区数据导出元数据。
- `import_regions.py`：MongoDB 地区数据导入脚本。

## 导入命令

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

## 导入后校验

```bash
python3 - <<'PY'
from pymongo import MongoClient

client = MongoClient("mongodb://root:123456@localhost:27017/medicine?authSource=admin")
print(client["medicine"]["regions"].count_documents({}))
client.close()
PY
```
