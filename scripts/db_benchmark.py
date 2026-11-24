# Data-layer benchmark: seeds MySQL with products and compares
# MySQL vs Redis lookups, category query with/without index, and stock update strategies.
# needs: pip install pymysql redis, a MySQL user bench/bench with database shopnest, and Redis on localhost
import json, random, statistics, threading, time
import pymysql, redis

def conn():
    return pymysql.connect(host="localhost", user="bench", password="bench", database="shopnest", autocommit=True)

c = conn(); cur = c.cursor()
r = redis.Redis()
N_PRODUCTS = 200_000
N_CAT = 50

def setup():
    cur.execute("DROP TABLE IF EXISTS products"); cur.execute("DROP TABLE IF EXISTS categories")
    cur.execute("""CREATE TABLE categories (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) UNIQUE,
                   description VARCHAR(255), created_at DATETIME(6), updated_at DATETIME(6))""")
    cur.execute("""CREATE TABLE products (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL,
                   description VARCHAR(2000), price DECIMAL(12,2) NOT NULL, stock INT, image_url VARCHAR(255),
                   category_id BIGINT, version BIGINT, created_at DATETIME(6), updated_at DATETIME(6))""")
    cur.executemany("INSERT INTO categories(name,description,created_at) VALUES(%s,%s,NOW())",
                    [(f"Category {i}", "desc") for i in range(N_CAT)])
    batch = []
    for i in range(N_PRODUCTS):
        batch.append((f"Product {i} {random.choice(['mouse','keyboard','phone','shirt','book','lamp'])}",
                      "Lorem ipsum " * 20, round(random.uniform(99, 99999), 2), random.randint(0, 500),
                      random.randint(1, N_CAT)))
        if len(batch) == 5000:
            cur.executemany("INSERT INTO products(name,description,price,stock,category_id,version,created_at) VALUES(%s,%s,%s,%s,%s,0,NOW())", batch)
            batch = []

GET_SQL = """SELECT p.id,p.name,p.description,p.price,p.stock,p.image_url,c.id,c.name
             FROM products p LEFT JOIN categories c ON c.id=p.category_id WHERE p.id=%s"""

def db_get(pid):
    cur.execute(GET_SQL, (pid,))
    row = cur.fetchone()
    return json.dumps({"id": row[0], "name": row[1], "description": row[2], "price": str(row[3]),
                       "stock": row[4], "imageUrl": row[5], "categoryId": row[6], "categoryName": row[7]})

def cached_get(pid):
    v = r.get(f"shopnest:product::{pid}")
    if v is None:
        v = db_get(pid)
        r.setex(f"shopnest:product::{pid}", 600, v)
    return v

def measure(fn, args, warm=200):
    for a in args[:warm]: fn(a)
    t = []
    for a in args:
        s = time.perf_counter(); fn(a); t.append((time.perf_counter() - s) * 1000)
    t.sort()
    return {"avg": statistics.mean(t), "p50": t[len(t)//2], "p95": t[int(len(t)*.95)], "p99": t[int(len(t)*.99)]}

def run():
    res = {}
    hot = [random.randint(1, 1000) for _ in range(5000)]  # popular products
    r.flushall()
    res["product_db"] = measure(db_get, hot)
    for pid in set(hot): cached_get(pid)
    res["product_cache"] = measure(cached_get, hot)

    PAGE_SQL = """SELECT p.id,p.name,p.price,p.stock,c.name FROM products p LEFT JOIN categories c ON c.id=p.category_id
                  WHERE p.category_id=%s ORDER BY p.id LIMIT 10 OFFSET %s"""
    COUNT_SQL = "SELECT COUNT(*) FROM products WHERE category_id=%s"
    def page(arg):
        cid, off = arg
        cur.execute(PAGE_SQL, (cid, off)); cur.fetchall()
        cur.execute(COUNT_SQL, (cid,)); cur.fetchone()
    args = [(random.randint(1, N_CAT), random.randint(0, 20) * 10) for _ in range(300)]
    res["category_page_no_index"] = measure(page, args, warm=20)
    cur.execute("CREATE INDEX idx_product_category ON products(category_id)")
    res["category_page_index"] = measure(page, args, warm=20)
    cur.execute("EXPLAIN SELECT COUNT(*) FROM products WHERE category_id=7"); res["explain_after"] = str(cur.fetchone())
    cur.execute("DROP INDEX idx_product_category ON products")
    cur.execute("EXPLAIN SELECT COUNT(*) FROM products WHERE category_id=7"); res["explain_before"] = str(cur.fetchone())
    cur.execute("CREATE INDEX idx_product_category ON products(category_id)")

    res["stock_naive"] = stock_test(naive=True)
    res["stock_atomic"] = stock_test(naive=False)
    return res

def stock_test(naive, buyers=100, stock=10):
    cur.execute("UPDATE products SET stock=%s WHERE id=1", (stock,))
    ok = [0]; lock = threading.Lock()
    def buy():
        cc = conn(); k = cc.cursor()
        if naive:
            k.execute("SELECT stock FROM products WHERE id=1"); s = k.fetchone()[0]
            time.sleep(0.002)
            if s >= 1:
                k.execute("UPDATE products SET stock=%s WHERE id=1", (s - 1,))
                with lock: ok[0] += 1
        else:
            k.execute("UPDATE products SET stock=stock-1 WHERE id=1 AND stock>=1")
            if k.rowcount == 1:
                with lock: ok[0] += 1
        cc.close()
    ts = [threading.Thread(target=buy) for _ in range(buyers)]
    [t.start() for t in ts]; [t.join() for t in ts]
    cur.execute("SELECT stock FROM products WHERE id=1")
    return {"buyers": buyers, "initial_stock": stock, "successful_orders": ok[0], "final_stock": cur.fetchone()[0]}

if __name__ == "__main__":
    t = time.time(); setup(); print("seeded in", round(time.time() - t, 1), "s")
    out = run()
    print(json.dumps(out, indent=2, default=str))
    json.dump(out, open("benchmark_results.json", "w"), indent=2, default=str)
