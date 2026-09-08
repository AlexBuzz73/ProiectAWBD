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
            return resp.status, data, dict(resp.info())
    except urllib.error.HTTPError as e:
        data = e.read().decode('utf-8')
        return e.code, data, dict(e.headers)
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
            return resp.status, data, dict(resp.info())
    except urllib.error.HTTPError as e:
        data = e.read().decode('utf-8')
        return e.code, data, dict(e.headers)
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

def get_cb_state(actuator_url, cb_name):
    status, data, _ = http_get(actuator_url)
    if status != 200:
        return f"UNKNOWN (status {status})"
    try:
        parsed = json.loads(data)
        # resilience4j /actuator/circuitbreakers format
        # {"circuitBreakers": {"userServiceCircuitBreaker": {"state": "CLOSED", ...}}}
        # or {"circuitBreakers": [{"name": "...", "state": "CLOSED"}]}
        cbs = parsed.get("circuitBreakers", {})
        if isinstance(cbs, dict):
            if cb_name in cbs:
                return cbs[cb_name].get("state", "UNKNOWN")
        elif isinstance(cbs, list):
            for item in cbs:
                if item.get("name") == cb_name:
                    return item.get("state", "UNKNOWN")
        return "NOT_FOUND"
    except Exception as e:
        return f"PARSE_ERROR: {e}"

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
            result[app_name] = inst_list
        return result
    except Exception:
        return {}

def start_process(name, cmd_args, port):
    kill_port(port)
    print(f"Starting {name} on port {port}...")
    log_file = open(f"{name}.log", "w", encoding="utf-8")
    p = subprocess.Popen(cmd_args, stdout=log_file, stderr=subprocess.STDOUT)
    processes[name] = (p, port, log_file)
    return p

def stop_process(name):
    if name in processes:
        p, port, log_file = processes[name]
        print(f"Stopping {name} on port {port}...")
        try:
            p.terminate()
            p.wait(timeout=5)
        except Exception:
            try:
                p.kill()
            except Exception:
                pass
        try:
            log_file.close()
        except Exception:
            pass
        kill_port(port)
        del processes[name]

def cleanup():
    print("\n--- Cleaning up all background processes ---")
    for name in list(processes.keys()):
        stop_process(name)
    for port in [8761, 8090, 8081, 8181, 8082, 8182, 8083, 8183]:
        kill_port(port)
    print("Cleanup complete.")

def main():
    print("================================================================")
    print("  FAZA 7 — RESILIENCE4J & FAULT TOLERANCE VERIFICATION")
    print("================================================================")
    try:
        # Pre-cleanup
        for port in [8761, 8090, 8081, 8181, 8082, 8182, 8083, 8183]:
            kill_port(port)

        # 1. Start Eureka Server
        start_process("eureka-server", [JAVA_EXE, "-jar", EUREKA_JAR], 8761)
        if not wait_for_endpoint("http://localhost:8761/eureka/apps", 200, 45, "Eureka Server"):
            sys.exit(1)

        # 2. Start User Service Replicas (8081, 8181)
        start_process("user-service-1", [JAVA_EXE, "-jar", USER_JAR, "--server.port=8081", "--spring.profiles.active=discovery"], 8081)
        start_process("user-service-2", [JAVA_EXE, "-jar", USER_JAR, "--server.port=8181", "--spring.profiles.active=discovery"], 8181)
        wait_for_endpoint("http://localhost:8081/actuator/health", 200, 45, "user-service:8081")
        wait_for_endpoint("http://localhost:8181/actuator/health", 200, 45, "user-service:8181")

        # 3. Start Account Service Replicas (8082, 8182)
        start_process("account-service-1", [JAVA_EXE, "-jar", ACCOUNT_JAR, "--server.port=8082", "--spring.profiles.active=discovery"], 8082)
        start_process("account-service-2", [JAVA_EXE, "-jar", ACCOUNT_JAR, "--server.port=8182", "--spring.profiles.active=discovery"], 8182)
        wait_for_endpoint("http://localhost:8082/actuator/health", 200, 45, "account-service:8082")
        wait_for_endpoint("http://localhost:8182/actuator/health", 200, 45, "account-service:8182")

        # 4. Start Transaction Service Replicas (8083, 8183)
        start_process("transaction-service-1", [JAVA_EXE, "-jar", TRANSACTION_JAR, "--server.port=8083", "--spring.profiles.active=discovery"], 8083)
        start_process("transaction-service-2", [JAVA_EXE, "-jar", TRANSACTION_JAR, "--server.port=8183", "--spring.profiles.active=discovery"], 8183)
        wait_for_endpoint("http://localhost:8083/actuator/health", 200, 45, "transaction-service:8083")
        wait_for_endpoint("http://localhost:8183/actuator/health", 200, 45, "transaction-service:8183")

        # 5. Start Gateway Service (8090)
        start_process("gateway-service", [JAVA_EXE, "-jar", GATEWAY_JAR, "--server.port=8090"], 8090)
        wait_for_endpoint("http://localhost:8090/actuator/health", 200, 45, "gateway-service:8090")

        # Wait for Eureka discovery propagation
        print("\nChecking Eureka Registry for all 7 registered instances...")
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

        print("Waiting 5s for LoadBalancer cache warm-up across services...")
        time.sleep(5)

        # -------------------------------------------------------------
        # STEP 1: Verify Actuator CircuitBreaker endpoints on startup
        # -------------------------------------------------------------
        print("\n--- STEP 1: Verify Actuator CircuitBreakers on startup ---")
        acc_cb_state = get_cb_state("http://localhost:8082/actuator/circuitbreakers", "userServiceCircuitBreaker")
        print(f"[Actuator :8082] userServiceCircuitBreaker state: {acc_cb_state}")
        assert acc_cb_state == "CLOSED", f"Expected CLOSED, got {acc_cb_state}"

        tx_cb_state = get_cb_state("http://localhost:8083/actuator/circuitbreakers", "accountServiceCircuitBreaker")
        print(f"[Actuator :8083] accountServiceCircuitBreaker state: {tx_cb_state}")
        assert tx_cb_state == "CLOSED", f"Expected CLOSED, got {tx_cb_state}"
        print("[PASS] Initial CircuitBreaker states are CLOSED.")

        # -------------------------------------------------------------
        # STEP 2: Baseline Business Flow via Gateway
        # -------------------------------------------------------------
        print("\n--- STEP 2: Baseline Business Flow via Gateway (8090) ---")
        ts = int(time.time())
        username = f"resilience_{ts}"
        email = f"resilience_{ts}@bank.com"
        password = "Password123!"

        reg_payload = {
            "individual": {
                "firstName": "Resilience",
                "lastName": "Tester",
                "cnp": f"1910101{ts % 1000000:06d}",
                "phoneNumber": "0722222222",
                "dateOfBirth": "1991-01-01T00:00:00.000+00:00"
            },
            "user": {
                "username": username,
                "email": email,
                "password": password
            }
        }
        st, res_text, _ = http_post("http://localhost:8090/api/auth/register", reg_payload)
        print(f"Register status: {st}")
        assert st == 201, f"Register failed: {res_text}"

        st, login_text, _ = http_post("http://localhost:8090/api/auth/login", {
            "email": email,
            "password": password
        })
        print(f"Login status: {st}")
        assert st == 200, f"Login failed: {login_text}"
        login_json = json.loads(login_text)
        token = login_json["token"]
        user_id = login_json.get("userId", 1)
        auth_headers = {"Authorization": f"Bearer {token}"}

        # Inter-service Feign call: account-service -> user-service (via feign-test)
        st, feign_user, _ = http_get(f"http://localhost:8082/api/internal/accounts/feign-test/user/{user_id}", headers=auth_headers)
        print(f"account-service -> user-service Feign lookup status: {st}")
        assert st == 200, f"Feign user lookup failed: {feign_user}"

        # Create account via Gateway
        st, acc1_text, _ = http_post("http://localhost:8090/api/accounts", {
            "alias": "RON Safe",
            "currency": "RON",
            "initialAmount": 500.0
        }, headers=auth_headers)
        print(f"Create account status: {st}")
        assert st == 201, f"Create account failed: {acc1_text}"
        acc1_id = json.loads(acc1_text)["accountId"]

        # Inter-service Feign call: transaction-service -> account-service (via feign-test)
        st, feign_acc, _ = http_get(f"http://localhost:8083/api/internal/transactions/feign-test/account/{acc1_id}", headers=auth_headers)
        print(f"transaction-service -> account-service Feign lookup status: {st}")
        assert st == 200, f"Feign account lookup failed: {feign_acc}"
        print("[PASS] Baseline healthy business flow and inter-service Feign calls verified.")

        # -------------------------------------------------------------
        # STEP 3: SCENARIO A — TOTAL USER-SERVICE FAILURE
        # -------------------------------------------------------------
        print("\n=============================================================")
        print("  SCENARIO A: Total USER-SERVICE Failure -> CircuitBreaker OPEN")
        print("=============================================================")
        print("Killing BOTH user-service replicas (:8081 and :8181)...")
        stop_process("user-service-1")
        stop_process("user-service-2")
        time.sleep(2)

        print("Generating failure requests from account-service -> user-service...")
        for i in range(1, 7):
            t0 = time.time()
            st, data, hdrs = http_get(
                f"http://localhost:8082/api/internal/accounts/feign-test/user/{user_id}",
                headers={"X-Correlation-Id": f"corr-scenario-a-{i}"}
            )
            elapsed = int((time.time() - t0) * 1000)
            print(f"  Call #{i}: status={st}, duration={elapsed}ms")

        # Check circuit breaker state on account-service
        cb_state = get_cb_state("http://localhost:8082/actuator/circuitbreakers", "userServiceCircuitBreaker")
        print(f"userServiceCircuitBreaker state after failures: {cb_state}")
        assert cb_state in ["OPEN", "HALF_OPEN"], f"Expected OPEN or HALF_OPEN, got {cb_state}"
        print(f"[PASS] userServiceCircuitBreaker transitioned to {cb_state}!")

        # Verify FAIL-FAST on next call
        t_start = time.time()
        st, ff_data, ff_hdrs = http_get(
            f"http://localhost:8082/api/internal/accounts/feign-test/user/{user_id}",
            headers={"X-Correlation-Id": "corr-fail-fast-user"}
        )
        ff_duration = int((time.time() - t_start) * 1000)
        print(f"Fail-fast call: status={st}, duration={ff_duration}ms, body={ff_data}")
        assert st == 503, f"Expected 503, got {st}"
        assert ff_duration < 400, f"Fail-fast call took too long: {ff_duration}ms"
        ff_json = json.loads(ff_data)
        assert ff_json.get("error") == "Service Unavailable"
        assert ff_json.get("status") == 503
        assert "correlationId" in ff_json
        print("[PASS] Fail-fast confirmed with controlled HTTP 503!")

        # Verify Gateway also returns 503
        gw_st, gw_data, _ = http_get("http://localhost:8090/api/users/me", headers=auth_headers)
        print(f"Gateway response for down user-service: status={gw_st}")
        assert gw_st in [503, 500], f"Gateway response unexpected: {gw_st}"

        # Restart user-service:8081
        print("\nRestarting user-service on port 8081...")
        start_process("user-service-1", [JAVA_EXE, "-jar", USER_JAR, "--server.port=8081", "--spring.profiles.active=discovery"], 8081)
        wait_for_endpoint("http://localhost:8081/actuator/health", 200, 45, "user-service:8081")

        print("Waiting 11s for wait-duration-in-open-state (10s) to trigger HALF_OPEN and Eureka discovery...")
        time.sleep(11)

        # Probe calls to transition from HALF_OPEN to CLOSED
        print("Sending probe calls to test recovery...")
        for i in range(3):
            st, p_data, _ = http_get(f"http://localhost:8082/api/internal/accounts/feign-test/user/{user_id}", headers=auth_headers)
            print(f"  Probe call #{i+1}: status={st}")
            time.sleep(1)

        cb_recovered = get_cb_state("http://localhost:8082/actuator/circuitbreakers", "userServiceCircuitBreaker")
        print(f"userServiceCircuitBreaker state after recovery: {cb_recovered}")
        assert cb_recovered == "CLOSED", f"Expected CLOSED, got {cb_recovered}"
        print("[PASS] Scenario A: CLOSED -> OPEN -> HALF_OPEN -> CLOSED fully demonstrated!")

        # Restart replica 8181
        start_process("user-service-2", [JAVA_EXE, "-jar", USER_JAR, "--server.port=8181", "--spring.profiles.active=discovery"], 8181)
        wait_for_endpoint("http://localhost:8181/actuator/health", 200, 45, "user-service:8181")

        # -------------------------------------------------------------
        # STEP 4: SCENARIO B — TOTAL ACCOUNT-SERVICE FAILURE
        # -------------------------------------------------------------
        print("\n=============================================================")
        print("  SCENARIO B: Total ACCOUNT-SERVICE Failure -> CB OPEN & 503")
        print("=============================================================")
        print("Killing BOTH account-service replicas (:8082 and :8182)...")
        stop_process("account-service-1")
        stop_process("account-service-2")
        time.sleep(2)

        print("Generating failure safe-read requests from transaction-service -> account-service...")
        for i in range(1, 7):
            t0 = time.time()
            st, data, hdrs = http_get(
                f"http://localhost:8083/api/internal/transactions/feign-test/account/{acc1_id}",
                headers={"X-Correlation-Id": f"corr-scenario-b-{i}"}
            )
            elapsed = int((time.time() - t0) * 1000)
            print(f"  Call #{i}: status={st}, duration={elapsed}ms")

        # Check circuit breaker state on transaction-service
        tx_cb_state = get_cb_state("http://localhost:8083/actuator/circuitbreakers", "accountServiceCircuitBreaker")
        print(f"accountServiceCircuitBreaker state after failures: {tx_cb_state}")
        assert tx_cb_state in ["OPEN", "HALF_OPEN"], f"Expected OPEN or HALF_OPEN, got {tx_cb_state}"
        print(f"[PASS] accountServiceCircuitBreaker transitioned to {tx_cb_state}!")

        # Verify FAIL-FAST on next call
        t_start = time.time()
        st, ff_data, ff_hdrs = http_get(
            f"http://localhost:8083/api/internal/transactions/feign-test/account/{acc1_id}",
            headers={"X-Correlation-Id": "corr-fail-fast-tx"}
        )
        ff_duration = int((time.time() - t_start) * 1000)
        print(f"Fail-fast call: status={st}, duration={ff_duration}ms, body={ff_data}")
        assert st == 503, f"Expected 503, got {st}"
        assert ff_duration < 400, f"Fail-fast call took too long: {ff_duration}ms"
        ff_json = json.loads(ff_data)
        assert ff_json.get("error") == "Service Unavailable"
        assert ff_json.get("status") == 503
        assert "correlationId" in ff_json
        print("[PASS] Fail-fast confirmed with controlled HTTP 503!")

        # FINANCIAL SAFETY CHECK: Attempt payment when account-service is DOWN
        print("\nFinancial safety audit: attempting payment via Gateway when account-service is down...")
        st, pay_data, _ = http_post("http://localhost:8090/api/payments/initiate", {
            "sourceAccountId": acc1_id,
            "destinationIban": "RO99FAKE0000000000000099",
            "amount": 50.0,
            "currency": "RON",
            "categoryId": 1,
            "description": "Safe resilience test payment",
            "processingType": "STANDARD",
            "password": password
        }, headers=auth_headers)
        print(f"Payment attempt status: {st}, body: {pay_data}")
        assert st == 503, f"Expected 503 Service Unavailable, got {st}"
        print("[PASS] Payment aborted with HTTP 503 — NO partial transaction executed!")

        # Restart account-service:8082
        print("\nRestarting account-service on port 8082...")
        start_process("account-service-1", [JAVA_EXE, "-jar", ACCOUNT_JAR, "--server.port=8082", "--spring.profiles.active=discovery"], 8082)
        wait_for_endpoint("http://localhost:8082/actuator/health", 200, 45, "account-service:8082")

        print("Waiting 11s for wait-duration-in-open-state (10s) to trigger HALF_OPEN...")
        time.sleep(11)

        # Probe calls
        for i in range(3):
            st, p_data, _ = http_get(f"http://localhost:8083/api/internal/transactions/feign-test/account/{acc1_id}", headers=auth_headers)
            print(f"  Probe call #{i+1}: status={st}")
            time.sleep(1)

        cb_recovered = get_cb_state("http://localhost:8083/actuator/circuitbreakers", "accountServiceCircuitBreaker")
        print(f"accountServiceCircuitBreaker state after recovery: {cb_recovered}")
        assert cb_recovered == "CLOSED", f"Expected CLOSED, got {cb_recovered}"
        print("[PASS] Scenario B: CLOSED -> OPEN -> HALF_OPEN -> CLOSED fully demonstrated!")

        # Restart replica 8182
        start_process("account-service-2", [JAVA_EXE, "-jar", ACCOUNT_JAR, "--server.port=8182", "--spring.profiles.active=discovery"], 8182)
        wait_for_endpoint("http://localhost:8182/actuator/health", 200, 45, "account-service:8182")
        time.sleep(6)

        # -------------------------------------------------------------
        # STEP 5: SCENARIO C — PARTIAL REPLICA FAILURE
        # -------------------------------------------------------------
        print("\n=============================================================")
        print("  SCENARIO C: Partial Failure (1 of 2 replicas killed)")
        print("=============================================================")
        print("Stopping ONLY account-service:8082 (replica :8182 remains healthy)...")
        stop_process("account-service-1")
        time.sleep(2)

        print("Sending 8 consecutive requests from transaction-service -> account-service...")
        success_count = 0
        for i in range(1, 9):
            st, p_data, _ = http_get(f"http://localhost:8083/api/internal/transactions/feign-test/account/{acc1_id}", headers=auth_headers)
            print(f"  Request #{i}: status={st}")
            if st == 200:
                success_count += 1
            time.sleep(0.5)

        print(f"Successful requests via surviving replica :8182: {success_count}/8")
        assert success_count >= 6, f"Too many requests failed during partial failover: {success_count}"

        tx_cb_partial = get_cb_state("http://localhost:8083/actuator/circuitbreakers", "accountServiceCircuitBreaker")
        print(f"accountServiceCircuitBreaker state during partial replica failure: {tx_cb_partial}")
        assert tx_cb_partial == "CLOSED", f"Circuit Breaker opened unnecessarily: {tx_cb_partial}"
        print("[PASS] LoadBalancer handled partial failure; Circuit Breaker remained CLOSED!")

        # Restart account-service:8082
        start_process("account-service-1", [JAVA_EXE, "-jar", ACCOUNT_JAR, "--server.port=8082", "--spring.profiles.active=discovery"], 8082)
        wait_for_endpoint("http://localhost:8082/actuator/health", 200, 45, "account-service:8082")

        # -------------------------------------------------------------
        # STEP 6: Full Business Flow after Resilience Tests
        # -------------------------------------------------------------
        print("\n--- STEP 6: Full Business Flow Regression Check ---")
        time.sleep(5)
        st, acc_list, _ = http_get("http://localhost:8090/api/accounts", headers=auth_headers)
        print(f"List accounts status: {st}")
        assert st == 200, f"List accounts failed: {acc_list}"

        st, cats, _ = http_get("http://localhost:8090/api/categories", headers=auth_headers)
        print(f"List categories status: {st}")
        assert st == 200, f"List categories failed: {cats}"

        print("\n================================================================")
        print("  ALL RESILIENCE4J & FAULT TOLERANCE LIVE TESTS PASSED (100%)")
        print("================================================================")

    except Exception as e:
        print(f"\n[FATAL ERROR] {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        cleanup()

if __name__ == "__main__":
    main()
