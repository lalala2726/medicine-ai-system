local now_ms = tonumber(ARGV[1])
local request_member = ARGV[2]
local rule_count = tonumber(ARGV[3])
local expire_padding = tonumber(ARGV[4])

local windows_ms = {}
local limits = {}
local arg_index = 5
for i = 1, rule_count do
    windows_ms[i] = tonumber(ARGV[arg_index])
    arg_index = arg_index + 1
    limits[i] = tonumber(ARGV[arg_index])
    arg_index = arg_index + 1
end

local blocked = 0
local max_retry_ms = 0

for i = 1, rule_count do
    local key = KEYS[i]
    local window_ms = windows_ms[i]
    local limit = limits[i]
    local cutoff = now_ms - window_ms

    redis.call("ZREMRANGEBYSCORE", key, "-inf", cutoff)
    local current_count = redis.call("ZCARD", key)

    if current_count >= limit then
        blocked = 1
        local oldest_entry = redis.call("ZRANGE", key, 0, 0, "WITHSCORES")
        local retry_ms = window_ms
        if oldest_entry[2] ~= nil then
            retry_ms = tonumber(oldest_entry[2]) + window_ms - now_ms
            if retry_ms < 0 then
                retry_ms = 0
            end
        end
        if retry_ms > max_retry_ms then
            max_retry_ms = retry_ms
        end
    end
end

if blocked == 0 then
    for i = 1, rule_count do
        local key = KEYS[i]
        redis.call("ZADD", key, now_ms, request_member)
        local ttl_seconds = math.floor(windows_ms[i] / 1000) + expire_padding
        redis.call("EXPIRE", key, ttl_seconds)
    end
end

local shortest_key = KEYS[1]
local shortest_window_ms = windows_ms[1]
local shortest_limit = limits[1]
local shortest_cutoff = now_ms - shortest_window_ms
redis.call("ZREMRANGEBYSCORE", shortest_key, "-inf", shortest_cutoff)
local shortest_count = redis.call("ZCARD", shortest_key)
local shortest_remaining = shortest_limit - shortest_count
if shortest_remaining < 0 then
    shortest_remaining = 0
end

local shortest_reset_ms = 0
local shortest_oldest = redis.call("ZRANGE", shortest_key, 0, 0, "WITHSCORES")
if shortest_oldest[2] ~= nil then
    shortest_reset_ms = tonumber(shortest_oldest[2]) + shortest_window_ms - now_ms
    if shortest_reset_ms < 0 then
        shortest_reset_ms = 0
    end
end

return {
    blocked == 0 and 1 or 0,
    math.ceil(max_retry_ms / 1000),
    shortest_limit,
    shortest_remaining,
    math.ceil(shortest_reset_ms / 1000)
}
