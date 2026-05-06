local key = KEYS[1]
local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local expire = tonumber(ARGV[4])

local data = redis.call("HMGET", key, "tokens", "last_time")
local tokens = tonumber(data[1])
local last_time = tonumber(data[2])

if not tokens then
    tokens = capacity
    last_time = now
    end

local elapsed = now - last_time
local new_tokens = elapsed * rate / 1000
tokens = math.min(capacity, tokens + new_tokens)

local allow = 0
if tokens >= 1  then
    tokens = tokens - 1
    allow = 1
    end

redis.call("HMSET", key, "tokens", tokens, "last_time", now)
redis.call("EXPIRE", key, expire)

return allow