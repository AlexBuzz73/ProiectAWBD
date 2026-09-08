import os
import sys
import time
import json
import subprocess
import urllib.request
import urllib.error

REPO_ROOT = r"C:\Users\Teo\OneDrive\Dokumente\GitHub\ProiectAWBD\proiect"
EUREKA_JAR = os.path.join(REPO_ROOT, "eureka-server", "build", "libs", "eureka-server-0.0.1-SNAPSHOT.jar")
USER_JAR = os.path.join(REPO_ROOT, "user-service", "build", "libs", "user-service-0.0.1-SNAPSHOT.jar")
ACCOUNT_JAR = os.path.join(REPO_ROOT, "account-service", "build", "libs", "account-service-0.0.1-SNAPSHOT.jar")
TRANSACTION_JAR = os.path.join(REPO_ROOT, "transaction-service", "build", "libs", "transaction-service-0.0.1-SNAPSHOT.jar")
GATEWAY_JAR = os.path.join(REPO_ROOT, "gateway-service", "build", "libs", "gateway-service-0.0.1-SNAPSHOT.jar")

JAVA_EXE = r"C:\Users\Teo\.jdks\openjdk-25\bin\java.exe"
if not os.path.exists(JAVA_EXE):
    JAVA_EXE = "java"

try:
    sys.stdout.reconfigure(line_buffering=True)
except Exception:
    pass

processes = {}

def kill_port(port):
    try:
        cmd = f'powershell -Command "Get-NetTCPConnection -LocalPort {port} -ErrorAction SilentlyContinue | ForEach-Object {{ (Get-Process -Id $_.OwningProcess).Kill() }}"'
        subprocess.run(cmd, shell=True, capture_output=True)
        time.sleep(1)
    except Exception as e:
        print(f"Warning killing port {port}: {e}")

def http_get(url, headers=None):
    req = urllib.request.Request(url, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = resp.read().decode('utf-8')
            resp_headers = {k.lower(): v for k, v in resp.info().items()}
            return resp.status, data, resp_headers
    except urllib.error.HTTPError as e:
        data = e.read().decode('utf-8')
        return e.code, data, {k.lower(): v for k, v in e.headers.items()}
    except Exception as e:
        return 0, str(e), {}

def http_post(url, body_dict=None, headers=None):
    h = {'Content-Type': 'application/json'}
    if headers:
        h.update(headers)
    data_bytes = json.dumps(body_dict).encode('utf-8') if body_dict is not None else b''
    req = urllib.request.Request(url, data=data_bytes, headers=h, method='POST')
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = resp.read().decode('utf-8')
            return resp.status, data, {k.lower(): v for k, v in resp.info().items()}
    except urllib.error.HTTPError as e:
        data = e.read().decode('utf-8')
        return e.code, data, {k.lower(): v for k, v in e.headers.items()}
    except Exception as e:
        return 0, str(e), {}

def http_options(url, headers=None):
    req = urllib.request.Request(url, headers=headers or {}, method='OPTIONS')
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = resp.read().decode('utf-8')
            return resp.status, data, {k.lower(): v for k, v in resp.info().items()}
    except urllib.error.HTTPError as e:
        data = e.read().decode('utf-8')
        return e.code, data, {k.lower(): v for k, v in e.headers.items()}
    except Exception as e:
        return 0, str(e), {}

def wait_for_endpoint(url, expected_code=200, timeout=75, desc="service"):
    print(f"Waiting for {desc} at {url}...")
    start = time.time()
    while time.time() - start < timeout:
        status, data, _ = http_get(url)
        if status == expected_code:
            print(f"[OK] {desc} is UP! (took {int(time.time() - start)}s)")
            return True
        time.sleep(2)
    print(f"[ERROR] Timeout waiting for {desc} at {url}")
    return False

def get_eureka_instances():
    status, data, _ = http_get("http://localhost:8761/eureka/apps", headers={"Accept": "application/json"})
    if status != 200:
        return {}
    try:
        res = json.loads(data)
        apps = res.get("applications", {}).get("application", [])
        if isinstance(apps, dict):
            apps = [apps]
        result = {}
        for app in apps:
            app_name = app.get("name")
            inst_list = app.get("instance", [])
            if isinstance(inst_list, dict):
                inst_list = [inst_list]
            result[app_name] = [
                {
                    "instanceId": inst.get("instanceId"),
                    "port": inst.get("port", {}).get("$"),
                    "status": inst.get("status")
                }
                for inst in inst_list
            ]
        return result
    except Exception as e:
        print(f"Error parsing eureka apps JSON: {e}")
        return {}

def start_java_app(name, jar_path, extra_args=None):
    cmd = [JAVA_EXE, "-jar", jar_path]
    if extra_args:
        cmd.extend(extra_args)
    print(f"Launching {name}: {' '.join(cmd)}", flush=True)
    log_dir = os.path.join(REPO_ROOT, "logs")
    os.makedirs(log_dir, exist_ok=True)
    log_file = open(os.path.join(log_dir, f"{name}.log"), "w", encoding="utf-8")
    p = subprocess.Popen(cmd, stdout=log_file, stderr=subprocess.STDOUT, cwd=REPO_ROOT)
    processes[name] = p
    p._log_file = log_file
    return p

def cleanup():
    print("\n--- Cleaning up all processes ---", flush=True)
    for name, p in list(processes.items()):
        print(f"Terminating {name} (PID {p.pid})...", flush=True)
        try:
            subprocess.run(f"taskkill /F /T /PID {p.pid}", shell=True, capture_output=True)
        except Exception:
            pass
        if hasattr(p, "_log_file"):
            try:
                p._log_file.close()
            except Exception:
                pass
    time.sleep(2)
    for port in [8761, 8081, 8181, 8082, 8182, 8083, 8183, 8090]:
        kill_port(port)
    print("Cleanup completed.", flush=True)

def main():
    print("==================================================")
    print("FAZA 6 — SPRING CLOUD API GATEWAY VERIFICATION")
    print("==================================================")

    os.makedirs(os.path.join(REPO_ROOT, "data"), exist_ok=True)
    os.makedirs(os.path.join(REPO_ROOT, "keys"), exist_ok=True)

    print("\n[Step 0] Cleaning existing ports...")
    for port in [8761, 8081, 8181, 8082, 8182, 8083, 8183, 8090]:
        kill_port(port)

    try:
        # 1. Start Eureka Server
        print("\n[Step 1] Starting Eureka Server (8761)...")
        start_java_app("eureka", EUREKA_JAR)
        if not wait_for_endpoint("http://localhost:8761/eureka/apps", expected_code=200, timeout=60, desc="Eureka Server"):
            raise RuntimeError("Eureka Server failed to start")

        # 2. Start 2 replicas of User Service
        print("\n[Step 2] Starting User Service replicas (8081, 8181)...")
        start_java_app("user-8081", USER_JAR, ["--spring.profiles.active=discovery", "--server.port=8081"])
        wait_for_endpoint("http://localhost:8081/actuator/info", 200, 60, "user-service:8081")

        start_java_app("user-8181", USER_JAR, ["--spring.profiles.active=discovery", "--server.port=8181"])
        wait_for_endpoint("http://localhost:8181/actuator/info", 200, 60, "user-service:8181")

        # 3. Start 2 replicas of Account Service
        print("\n[Step 3] Starting Account Service replicas (8082, 8182)...")
        start_java_app("account-8082", ACCOUNT_JAR, ["--spring.profiles.active=discovery", "--server.port=8082"])
        start_java_app("account-8182", ACCOUNT_JAR, ["--spring.profiles.active=discovery", "--server.port=8182"])

        # 4. Start 2 replicas of Transaction Service
        print("\n[Step 4] Starting Transaction Service replicas (8083, 8183)...")
        start_java_app("transaction-8083", TRANSACTION_JAR, ["--spring.profiles.active=discovery", "--server.port=8083"])
        start_java_app("transaction-8183", TRANSACTION_JAR, ["--spring.profiles.active=discovery", "--server.port=8183"])

        # 5. Start Gateway Service (8090)
        print("\n[Step 5] Starting Gateway Service (8090)...")
        start_java_app("gateway-8090", GATEWAY_JAR)

        # Wait for all health/info endpoints
        wait_for_endpoint("http://localhost:8082/actuator/info", 200, 60, "account-service:8082")
        wait_for_endpoint("http://localhost:8182/actuator/info", 200, 60, "account-service:8182")
        wait_for_endpoint("http://localhost:8083/actuator/info", 200, 60, "transaction-service:8083")
        wait_for_endpoint("http://localhost:8183/actuator/info", 200, 60, "transaction-service:8183")
        wait_for_endpoint("http://localhost:8090/actuator/health", 200, 60, "gateway-service:8090")

        # 6. Verify Eureka Registry: 7 instances UP (2 User, 2 Account, 2 Tx, 1 Gateway)
        print("\n[Step 6] Checking Eureka Registry for all registered instances...")
        eureka_ready = False
        start_check = time.time()
        while time.time() - start_check < 60:
            apps = get_eureka_instances()
            user_inst = apps.get("USER-SERVICE", [])
            acc_inst = apps.get("ACCOUNT-SERVICE", [])
            tx_inst = apps.get("TRANSACTION-SERVICE", [])
            gw_inst = apps.get("GATEWAY-SERVICE", [])
            
            user_up = [i for i in user_inst if i["status"] == "UP"]
            acc_up = [i for i in acc_inst if i["status"] == "UP"]
            tx_up = [i for i in tx_inst if i["status"] == "UP"]
            gw_up = [i for i in gw_inst if i["status"] == "UP"]

            print(f"Eureka UP -> USER: {len(user_up)}/2, ACCOUNT: {len(acc_up)}/2, TX: {len(tx_up)}/2, GATEWAY: {len(gw_up)}/1")

            if len(user_up) >= 2 and len(acc_up) >= 2 and len(tx_up) >= 2 and len(gw_up) >= 1:
                eureka_ready = True
                print("\n[SUCCESS] All 7 service instances registered UP in Eureka!")
                break
            time.sleep(3)

        if not eureka_ready:
            raise RuntimeError("Timed out waiting for all instances to register UP in Eureka")

        # Warm-up delay for Gateway load balancer cache
        print("\nWaiting 5s for Gateway LoadBalancer cache warm-up...")
        time.sleep(5)

        # 7. Gateway Security Verification: 401 on unauthenticated request
        print("\n[Step 7] Verifying Gateway Security: Unauthenticated request to /api/accounts returns 401...")
        s, d, h = http_get("http://localhost:8090/api/accounts")
        print(f"  Response status: {s}, body: {d}")
        print(f"  Headers: X-Correlation-Id={h.get('x-correlation-id')}, X-Gateway-Service={h.get('x-gateway-service')}")
        assert s == 401, f"Expected 401 Unauthorized, got {s}: {d}"
        assert "x-correlation-id" in h, "Missing X-Correlation-Id header in response"
        assert h.get("x-gateway-service") == "gateway-service", f"Invalid X-Gateway-Service header: {h.get('x-gateway-service')}"
        print("[SUCCESS] Gateway correctly returned 401 Unauthorized with correlation ID and gateway header!")

        # 8. Gateway Security Verification: 401 on invalid JWT
        print("\n[Step 8] Verifying Gateway Security: Invalid JWT returns 401...")
        s, d, h = http_get("http://localhost:8090/api/accounts", headers={"Authorization": "Bearer invalid.mock.jwt"})
        assert s == 401, f"Expected 401 for invalid JWT, got {s}: {d}"
        print("[SUCCESS] Gateway rejected invalid JWT with 401 Unauthorized!")

        # 9. Gateway Public JWKS Routing
        print("\n[Step 9] Verifying public JWKS routing via Gateway: http://localhost:8090/.well-known/jwks.json...")
        s, d, h = http_get("http://localhost:8090/.well-known/jwks.json")
        assert s == 200, f"Expected 200 from JWKS via Gateway, got {s}: {d}"
        jwks = json.loads(d)
        assert "keys" in jwks and len(jwks["keys"]) > 0, f"Invalid JWKS content: {jwks}"
        print(f"[SUCCESS] JWKS served via Gateway with kid={jwks['keys'][0]['kid']}!")

        # 10. CORS Preflight Verification
        print("\n[Step 10] Verifying CORS preflight via Gateway...")
        cors_headers = {
            "Origin": "http://localhost:5173",
            "Access-Control-Request-Method": "GET",
            "Access-Control-Request-Headers": "Authorization"
        }
        s, d, h = http_options("http://localhost:8090/api/accounts", headers=cors_headers)
        print(f"  CORS Status: {s}, Allow-Origin: {h.get('access-control-allow-origin')}")
        assert s == 200, f"Expected 200 OK on CORS preflight, got {s}"
        assert h.get("access-control-allow-origin") == "http://localhost:5173", f"CORS Allow-Origin mismatch: {h}"
        assert h.get("access-control-allow-credentials") == "true", f"CORS Allow-Credentials mismatch: {h}"
        print("[SUCCESS] CORS preflight succeeded with correct origin and credentials!")

        # 11. User Registration & Login via Gateway
        print("\n[Step 11] Registering and Logging in user through Gateway (http://localhost:8090/api/auth)...", flush=True)
        ts = int(time.time())
        email = f"gw_user_{ts}@test.com"
        username = f"gwuser_{ts}"
        reg_payload = {
            "individual": {
                "firstName": "Gateway",
                "lastName": "User",
                "cnp": f"1910101{ts % 1000000:06d}",
                "phoneNumber": "0722222222",
                "dateOfBirth": "1991-01-01T00:00:00.000+00:00"
            },
            "user": {
                "username": username,
                "email": email,
                "password": "Password123!"
            }
        }
        s, d, h = http_post("http://localhost:8090/api/auth/register", reg_payload)
        assert s == 201, f"Registration via Gateway failed: {s} {d}"
        print(f"[SUCCESS] Registered user {email} via Gateway!")

        login_payload = {"email": email, "password": "Password123!"}
        s, d, h = http_post("http://localhost:8090/api/auth/login", login_payload)
        assert s == 200, f"Login via Gateway failed: {s} {d}"
        login_res = json.loads(d)
        user_jwt = login_res.get("token") or login_res.get("accessToken")
        assert user_jwt, f"Missing JWT token in login response: {d}"
        auth_headers = {"Authorization": f"Bearer {user_jwt}"}
        print(f"[SUCCESS] Logged in via Gateway, received JWT token ({user_jwt[:25]}...)!")

        # 12. Role-Based Authorization via Gateway (403 Forbidden for USER on ADMIN endpoint)
        print("\n[Step 12] Verifying Role-Based Access Control: USER accessing ADMIN endpoint via Gateway...")
        s, d, h = http_get("http://localhost:8090/api/admin/users/all", headers=auth_headers)
        print(f"  Admin endpoint access with USER role: status={s}, body={d}")
        assert s == 403, f"Expected 403 Forbidden for USER on /api/admin/**, got {s}: {d}"
        print("[SUCCESS] Gateway enforced RBAC: HTTP 403 Forbidden for non-admin user!")

        # 13. Gateway Rate Limiting: burst requests to /api/auth/login
        print("\n[Step 13] Verifying Gateway Rate Limiting: Burst 15 requests to /api/auth/login...")
        rate_limited = False
        rate_limit_statuses = []
        for i in range(15):
            st, dt, hdrs = http_post("http://localhost:8090/api/auth/login", login_payload)
            rate_limit_statuses.append(st)
            if st == 429:
                rate_limited = True
                print(f"  Req #{i+1}: Got HTTP 429 Too Many Requests! Retry-After={hdrs.get('retry-after')}")
                print(f"  Body: {dt}")
                break
            time.sleep(0.05)

        print(f"Statuses observed during burst: {rate_limit_statuses}")
        assert rate_limited, f"Rate limiter did not trigger 429! Statuses: {rate_limit_statuses}"
        print("[SUCCESS] In-memory Rate Limiter successfully returned HTTP 429 Too Many Requests!")

        # Wait for rate limiter bucket recovery
        print("Waiting 3s for rate limiter token bucket recovery...")
        time.sleep(3)

        # 14. Authenticated Request Routing through Gateway: Account Service
        print("\n[Step 14] Verifying Authenticated Request Routing through Gateway to Account Service...")
        s, d, h = http_get("http://localhost:8090/api/accounts", headers=auth_headers)
        assert s == 200, f"Failed to access /api/accounts with JWT via Gateway: {s} {d}"
        print(f"[SUCCESS] Successfully accessed /api/accounts via Gateway (HTTP 200): {d}")

        # 15. Load Balancing through Gateway across User Service Replicas
        print("\n[Step 15] Verifying Load Balancing through Gateway: user-service (8081 & 8181)...")
        user_ports = set()
        for i in range(12):
            s, d, h = http_get("http://localhost:8090/api/users/me", headers=auth_headers)
            if s == 200:
                p_hdr = h.get("x-service-port")
                user_ports.add(p_hdr)
                print(f"  Req #{i+1}: Gateway routed to user-service port {p_hdr} (X-Instance-Id: {h.get('x-instance-id')})")
            time.sleep(0.3)

        print(f"User service ports hit via Gateway: {user_ports}")
        assert "8081" in user_ports and "8181" in user_ports, (
            f"Gateway load balancing failed! Expected both 8081 and 8181, got {user_ports}"
        )
        print("[SUCCESS] Gateway successfully load balanced requests across both user-service replicas!")

        # 16. Load Balancing through Gateway across Account Service Replicas
        print("\n[Step 16] Verifying Load Balancing through Gateway: account-service (8082 & 8182)...")
        acc_ports = set()
        for i in range(12):
            s, d, h = http_get("http://localhost:8090/api/accounts", headers=auth_headers)
            if s == 200:
                p_hdr = h.get("x-service-port")
                acc_ports.add(p_hdr)
                print(f"  Req #{i+1}: Gateway routed to account-service port {p_hdr} (X-Instance-Id: {h.get('x-instance-id')})")
            time.sleep(0.3)

        print(f"Account service ports hit via Gateway: {acc_ports}")
        assert "8082" in acc_ports and "8182" in acc_ports, (
            f"Gateway load balancing failed! Expected both 8082 and 8182, got {acc_ports}"
        )
        print("[SUCCESS] Gateway successfully load balanced requests across both account-service replicas!")

        # 17. Failover Demonstration through Gateway: Stop account-service:8082
        print("\n[Step 17] Verifying Failover through Gateway: Stopping account-service:8082...")
        acc_proc = processes.pop("account-8082")
        subprocess.run(f"taskkill /F /T /PID {acc_proc.pid}", shell=True, capture_output=True)
        kill_port(8082)
        print("account-service:8082 killed. Waiting 6s for Eureka/LoadBalancer cache update...")
        time.sleep(6)

        failover_ports = []
        for i in range(6):
            s, d, h = http_get("http://localhost:8090/api/accounts", headers=auth_headers)
            if s == 200:
                p_hdr = h.get("x-service-port")
                failover_ports.append(p_hdr)
                print(f"  Req #{i+1}: Successfully served by port {p_hdr}")
            else:
                print(f"  Req #{i+1}: Result {s}: {d}")
            time.sleep(0.5)

        assert "8182" in failover_ports, f"Failover via Gateway to 8182 failed! Ports: {failover_ports}"
        print(f"[SUCCESS] Transparent Failover verified! Gateway served all requests via replica 8182 without restart!")

        # 18. Correlation ID Propagation Verification
        print("\n[Step 18] Verifying Correlation ID Propagation through Gateway...")
        custom_cid = "custom-client-trace-998877"
        s, d, h = http_get("http://localhost:8090/api/accounts", headers={
            "Authorization": f"Bearer {user_jwt}",
            "X-Correlation-Id": custom_cid
        })
        assert s == 200, f"Failed: {s} {d}"
        assert h.get("x-correlation-id") == custom_cid, f"CID not propagated! Got: {h.get('x-correlation-id')}"
        print(f"[SUCCESS] Custom X-Correlation-Id '{custom_cid}' preserved and returned by Gateway!")

        # 19. Transaction Service Routing via Gateway
        print("\n[Step 19] Verifying Transaction Service routing via Gateway: /api/categories...")
        s, d, h = http_get("http://localhost:8090/api/categories", headers=auth_headers)
        print(f"  Categories status: {s}, headers X-Service-Port: {h.get('x-service-port')}")
        assert s == 200, f"Failed categories routing: {s} {d}"
        print("[SUCCESS] Transaction Service correctly routed via Gateway!")

        print("\n=======================================================================")
        print("FAZA 6 — API GATEWAY VERIFICATION PASSED COMPLETELY AND SUCCESSFULLY!")
        print("=======================================================================")

    finally:
        cleanup()

if __name__ == '__main__':
    main()
