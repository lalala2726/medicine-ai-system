package com.zhangyichuang.medicine.shared.mongodb;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * MongoDB缓存操作工具类
 * 提供基础的CRUD操作和查询功能
 *
 * @author Chuang
 */
@Component
public final class MongoCache {

    private static final Logger log = LoggerFactory.getLogger(MongoCache.class);

    private final MongoTemplate mongoTemplate;

    public MongoCache(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 保存文档(如果文档已存在则更新,不存在则插入)
     *
     * @param document       文档对象
     * @param collectionName 集合名称
     * @param <T>            文档类型
     * @return 保存后的文档
     */
    public <T> T save(T document, String collectionName) {
        try {
            return mongoTemplate.save(document, collectionName);
        } catch (Exception e) {
            log.error("MongoDB save document failed, collection: {}, error: {}", collectionName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 插入单个文档
     *
     * @param document       文档对象
     * @param collectionName 集合名称
     * @param <T>            文档类型
     * @return 插入后的文档
     */
    public <T> T insert(T document, String collectionName) {
        try {
            return mongoTemplate.insert(document, collectionName);
        } catch (Exception e) {
            log.error("MongoDB insert document failed, collection: {}, error: {}", collectionName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量插入文档
     *
     * @param documents      文档列表
     * @param collectionName 集合名称
     * @param <T>            文档类型
     * @return 插入后的文档列表
     */
    public <T> Collection<T> insertBatch(Collection<T> documents, String collectionName) {
        try {
            return mongoTemplate.insert(documents, collectionName);
        } catch (Exception e) {
            log.error("MongoDB batch insert failed, collection: {}, count: {}, error: {}",
                    collectionName, documents.size(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据ID查询文档
     *
     * @param id             文档ID
     * @param clazz          文档类型
     * @param collectionName 集合名称
     * @param <T>            文档类型
     * @return 查询到的文档,不存在返回null
     */
    public <T> T findById(String id, Class<T> clazz, String collectionName) {
        try {
            return mongoTemplate.findById(id, clazz, collectionName);
        } catch (Exception e) {
            log.error("MongoDB find by id failed, collection: {}, id: {}, error: {}",
                    collectionName, id, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 查询单个文档
     *
     * @param query          查询条件
     * @param clazz          文档类型
     * @param collectionName 集合名称
     * @param <T>            文档类型
     * @return 查询到的文档,不存在返回null
     */
    public <T> T findOne(Query query, Class<T> clazz, String collectionName) {
        try {
            return mongoTemplate.findOne(query, clazz, collectionName);
        } catch (Exception e) {
            log.error("MongoDB find one failed, collection: {}, query: {}, error: {}",
                    collectionName, query, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 查询所有文档
     *
     * @param clazz          文档类型
     * @param collectionName 集合名称
     * @param <T>            文档类型
     * @return 文档列表
     */
    public <T> List<T> findAll(Class<T> clazz, String collectionName) {
        try {
            return mongoTemplate.findAll(clazz, collectionName);
        } catch (Exception e) {
            log.error("MongoDB find all failed, collection: {}, error: {}",
                    collectionName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 条件查询文档
     *
     * @param query          查询条件
     * @param clazz          文档类型
     * @param collectionName 集合名称
     * @param <T>            文档类型
     * @return 文档列表
     */
    public <T> List<T> find(Query query, Class<T> clazz, String collectionName) {
        try {
            return mongoTemplate.find(query, clazz, collectionName);
        } catch (Exception e) {
            log.error("MongoDB find failed, collection: {}, query: {}, error: {}",
                    collectionName, query, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 分页查询文档
     *
     * @param query          查询条件
     * @param page           页码(从0开始)
     * @param size           每页大小
     * @param clazz          文档类型
     * @param collectionName 集合名称
     * @param <T>            文档类型
     * @return 文档列表
     */
    public <T> List<T> findPage(Query query, int page, int size, Class<T> clazz, String collectionName) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            query.with(pageable);
            return mongoTemplate.find(query, clazz, collectionName);
        } catch (Exception e) {
            log.error("MongoDB find page failed, collection: {}, page: {}, size: {}, error: {}",
                    collectionName, page, size, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据ID更新文档
     *
     * @param id             文档ID
     * @param update         更新操作
     * @param collectionName 集合名称
     * @return 更新结果
     */
    public UpdateResult updateById(String id, Update update, String collectionName) {
        try {
            Query query = new Query();
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id));
            return mongoTemplate.updateFirst(query, update, collectionName);
        } catch (Exception e) {
            log.error("MongoDB update by id failed, collection: {}, id: {}, error: {}",
                    collectionName, id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 更新单个文档
     *
     * @param query          查询条件
     * @param update         更新操作
     * @param collectionName 集合名称
     * @return 更新结果
     */
    public UpdateResult updateOne(Query query, Update update, String collectionName) {
        try {
            return mongoTemplate.updateFirst(query, update, collectionName);
        } catch (Exception e) {
            log.error("MongoDB update one failed, collection: {}, query: {}, error: {}",
                    collectionName, query, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量更新文档
     *
     * @param query          查询条件
     * @param update         更新操作
     * @param collectionName 集合名称
     * @return 更新结果
     */
    public UpdateResult updateMulti(Query query, Update update, String collectionName) {
        try {
            return mongoTemplate.updateMulti(query, update, collectionName);
        } catch (Exception e) {
            log.error("MongoDB update multi failed, collection: {}, query: {}, error: {}",
                    collectionName, query, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据ID删除文档
     *
     * @param id             文档ID
     * @param collectionName 集合名称
     * @param clazz          文档类型
     * @param <T>            文档类型
     * @return 删除的文档对象,不存在返回null
     */
    public <T> T deleteById(String id, Class<T> clazz, String collectionName) {
        try {
            Query query = new Query();
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id));
            return mongoTemplate.findAndRemove(query, clazz, collectionName);
        } catch (Exception e) {
            log.error("MongoDB delete by id failed, collection: {}, id: {}, error: {}",
                    collectionName, id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 条件删除文档
     *
     * @param query          查询条件
     * @param collectionName 集合名称
     * @return 删除结果
     */
    public DeleteResult delete(Query query, String collectionName) {
        try {
            return mongoTemplate.remove(query, collectionName);
        } catch (Exception e) {
            log.error("MongoDB delete failed, collection: {}, query: {}, error: {}",
                    collectionName, query, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 统计文档数量
     *
     * @param query          查询条件
     * @param collectionName 集合名称
     * @return 文档数量
     */
    public long count(Query query, String collectionName) {
        try {
            return mongoTemplate.count(query, collectionName);
        } catch (Exception e) {
            log.error("MongoDB count failed, collection: {}, query: {}, error: {}",
                    collectionName, query, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 判断文档是否存在
     *
     * @param query          查询条件
     * @param collectionName 集合名称
     * @return true=存在；false=不存在
     */
    public boolean exists(Query query, String collectionName) {
        try {
            return mongoTemplate.exists(query, collectionName);
        } catch (Exception e) {
            log.error("MongoDB exists check failed, collection: {}, query: {}, error: {}",
                    collectionName, query, e.getMessage(), e);
            return false;
        }
    }
}
