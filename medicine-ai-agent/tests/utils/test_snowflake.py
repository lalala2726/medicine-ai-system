from app.utils.snowflake import MAX_NODE_ID, SnowflakeIdGenerator, generate_snowflake_id


def test_generate_snowflake_id_returns_positive_int() -> None:
    """验证默认雪花生成器返回正整数 ID。"""
    value = generate_snowflake_id()

    assert isinstance(value, int)
    assert value > 0


def test_snowflake_generator_produces_unique_ordered_ids() -> None:
    """验证同一生成器连续产出的 ID 唯一且递增。"""
    generator = SnowflakeIdGenerator(node_id=1)

    ids = [generator.next_id() for _ in range(5)]

    assert ids == sorted(ids)
    assert len(set(ids)) == 5


def test_snowflake_generator_rejects_invalid_node_id() -> None:
    """验证节点编号超范围时会直接拒绝初始化。"""
    try:
        SnowflakeIdGenerator(node_id=MAX_NODE_ID + 1)
    except ValueError as exc:
        assert "node_id" in str(exc)
    else:
        raise AssertionError("超范围 node_id 应抛出 ValueError")
