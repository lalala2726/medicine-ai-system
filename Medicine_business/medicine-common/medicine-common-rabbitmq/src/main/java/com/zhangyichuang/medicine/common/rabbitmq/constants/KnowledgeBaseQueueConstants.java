package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * 知识库导入相关的队列/交换机常量。
 */
public final class KnowledgeBaseQueueConstants {

    /**
     * 知识库导入交换机。
     */
    public static final String EXCHANGE = "knowledge.import";
    /**
     * 知识库导入文档队列，协议队列名沿用 command。
     */
    public static final String COMMAND_QUEUE = "knowledge.import.command.q";
    /**
     * 知识库导入 result 队列。
     */
    public static final String RESULT_QUEUE = "knowledge.import.result.q";

    /**
     * 知识库导入文档路由键，协议路由键沿用 command。
     */
    public static final String ROUTING_COMMAND = "knowledge.import.command";

    /**
     * 知识库导入 result 路由键。
     */
    public static final String ROUTING_RESULT = "knowledge.import.result";

    /**
     * @deprecated 使用 {@link #COMMAND_QUEUE}
     */
    @Deprecated
    public static final String QUEUE = COMMAND_QUEUE;

    /**
     * @deprecated 使用 {@link #ROUTING_COMMAND}
     */
    @Deprecated
    public static final String ROUTING_IMPORT = ROUTING_COMMAND;

    /**
     * 知识库向量删除队列。
     */
    public static final String VECTOR_DELETE_QUEUE = "kb.vector.delete.queue";
    /**
     * 知识库向量删除路由键。
     */
    public static final String ROUTING_VECTOR_DELETE = "kb.vector.delete";

    /**
     * 知识库删除队列。
     */
    public static final String KB_DELETE_QUEUE = "kb.delete.queue";
    /**
     * 知识库删除路由键。
     */
    public static final String ROUTING_KB_DELETE = "kb.delete";

    /**
     * 知识库切片更新队列。
     */
    public static final String CHUNK_UPDATE_QUEUE = "kb.chunk.update.queue";
    /**
     * 知识库切片更新路由键。
     */
    public static final String ROUTING_CHUNK_UPDATE = "kb.chunk.update";

    /**
     * 单切片重建交换机。
     */
    public static final String CHUNK_REBUILD_EXCHANGE = "knowledge.chunk_rebuild";

    /**
     * 单切片重建 command 队列。
     */
    public static final String CHUNK_REBUILD_COMMAND_QUEUE = "knowledge.chunk_rebuild.command.q";

    /**
     * 单切片重建 result 队列。
     */
    public static final String CHUNK_REBUILD_RESULT_QUEUE = "knowledge.chunk_rebuild.result.q";

    /**
     * 单切片重建 command 路由键。
     */
    public static final String ROUTING_CHUNK_REBUILD_COMMAND = "knowledge.chunk_rebuild.command";

    /**
     * 单切片重建 result 路由键。
     */
    public static final String ROUTING_CHUNK_REBUILD_RESULT = "knowledge.chunk_rebuild.result";

    /**
     * 文档切片新增交换机。
     */
    public static final String CHUNK_ADD_EXCHANGE = "knowledge.chunk_add";

    /**
     * 文档切片新增 command 队列。
     */
    public static final String CHUNK_ADD_COMMAND_QUEUE = "knowledge.chunk_add.command.q";

    /**
     * 文档切片新增 result 队列。
     */
    public static final String CHUNK_ADD_RESULT_QUEUE = "knowledge.chunk_add.result.q";

    /**
     * 文档切片新增 command 路由键。
     */
    public static final String ROUTING_CHUNK_ADD_COMMAND = "knowledge.chunk_add.command";

    /**
     * 文档切片新增 result 路由键。
     */
    public static final String ROUTING_CHUNK_ADD_RESULT = "knowledge.chunk_add.result";

    private KnowledgeBaseQueueConstants() {
    }
}
