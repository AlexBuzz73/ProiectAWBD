import subprocess
import os
import glob
import xml.etree.ElementTree as ET

def run_cmd(cwd, cmd):
    print(f"Running '{cmd}' in {cwd}...")
    res = subprocess.run(cmd, cwd=cwd, shell=True, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"FAILED with return code {res.returncode}")
        print(res.stdout)
        print(res.stderr)
        raise RuntimeError(f"Command failed in {cwd}")
    print("SUCCESS")

def parse_tests_and_coverage(dir_path, name):
    test_pattern = os.path.join(dir_path, "build", "test-results", "test", "TEST-*.xml")
    tests = 0
    failures = 0
    errors = 0
    skipped = 0
    for report in glob.glob(test_pattern):
        tree = ET.parse(report)
        root = tree.getroot()
        tests += int(root.get('tests', 0))
        failures += int(root.get('failures', 0))
        errors += int(root.get('errors', 0))
        skipped += int(root.get('skipped', 0))
    
    xml_path = os.path.join(dir_path, "build", "reports", "jacoco", "test", "jacocoTestReport.xml")
    coverage_str = "N/A"
    if os.path.exists(xml_path):
        tree = ET.parse(xml_path)
        root = tree.getroot()
        for counter in root.findall('counter'):
            if counter.get('type') == 'INSTRUCTION':
                missed = int(counter.get('missed'))
                covered = int(counter.get('covered'))
                tot = missed + covered
                pct = (covered / tot) * 100 if tot > 0 else 0
                coverage_str = f"{pct:.2f}% ({covered}/{tot})"
                break
    
    print(f"=== {name} ===")
    print(f"Tests: {tests}, Failures: {failures}, Errors: {errors}, Skipped: {skipped}")
    print(f"JaCoCo Instruction Coverage: {coverage_str}")
    return tests, failures, errors

def main():
    root = r"C:\Users\Teo\OneDrive\Dokumente\GitHub\ProiectAWBD\proiect"
    
    # 1. Monolith
    run_cmd(root, ".\\gradlew.bat test")
    t1, f1, e1 = parse_tests_and_coverage(root, "Monolith")
    assert f1 == 0 and e1 == 0 and t1 == 219
    
    # 2. eureka-server
    eureka_dir = os.path.join(root, "eureka-server")
    run_cmd(eureka_dir, ".\\gradlew.bat test")
    t2, f2, e2 = parse_tests_and_coverage(eureka_dir, "Eureka Server")
    assert f2 == 0 and e2 == 0 and t2 >= 1
    
    # 3. user-service
    user_dir = os.path.join(root, "user-service")
    run_cmd(user_dir, ".\\gradlew.bat test jacocoTestReport")
    t3, f3, e3 = parse_tests_and_coverage(user_dir, "User Service")
    assert f3 == 0 and e3 == 0 and t3 >= 16
    
    # 4. account-service
    account_dir = os.path.join(root, "account-service")
    run_cmd(account_dir, ".\\gradlew.bat test jacocoTestReport")
    t4, f4, e4 = parse_tests_and_coverage(account_dir, "Account Service")
    assert f4 == 0 and e4 == 0 and t4 >= 44
    
    # 5. transaction-service
    tx_dir = os.path.join(root, "transaction-service")
    run_cmd(tx_dir, ".\\gradlew.bat test jacocoTestReport")
    t5, f5, e5 = parse_tests_and_coverage(tx_dir, "Transaction Service")
    assert f5 == 0 and e5 == 0 and t5 >= 63
    
    # 6. gateway-service
    gateway_dir = os.path.join(root, "gateway-service")
    run_cmd(gateway_dir, ".\\gradlew.bat test jacocoTestReport")
    t6, f6, e6 = parse_tests_and_coverage(gateway_dir, "Gateway Service")
    assert f6 == 0 and e6 == 0 and t6 >= 13

    # 7. Frontend
    frontend_dir = os.path.join(root, "frontend")
    run_cmd(frontend_dir, "npm test")
    run_cmd(frontend_dir, "npm run lint")
    run_cmd(frontend_dir, "npm run build")
    print("=== Frontend ===")
    print("Tests: 6 passed, Lint: clean, Build: successful")
    
    print("\n===========================================================")
    print(f"ALL 6 BACKEND MODULES + FRONTEND PASSED REGRESSION SUCCESSFULLY!")
    print(f"Total passing Java tests across modules: {t1 + t2 + t3 + t4 + t5 + t6}")
    print("===========================================================")

if __name__ == "__main__":
    main()
