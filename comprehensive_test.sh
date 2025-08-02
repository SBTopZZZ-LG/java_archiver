#!/bin/bash

# Comprehensive test suite for Java Archiver improvements
cd /home/runner/work/java_archiver/java_archiver

echo "Comprehensive Java Archiver Test Suite"
echo "======================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

passed_tests=0
total_tests=0

test_passed() {
    echo -e "${GREEN}✓ $1${NC}"
    ((passed_tests++))
    ((total_tests++))
}

test_failed() {
    echo -e "${RED}✗ $1${NC}"
    ((total_tests++))
}

test_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Clean up from previous runs
rm -f *.archivit
rm -rf *_test

echo
echo "Phase 1: Basic Functionality Tests"
echo "=================================="

# Test 1: Original interactive archiver still works
test_info "Testing original Main.java compilation..."
if javac -cp ".:commons-io-2.11.0.jar" -d out/production/Archivit src/Main.java 2>/dev/null; then
    test_passed "Original Main.java compiles successfully"
else
    test_failed "Original Main.java compilation failed"
fi

# Test 2: Enhanced non-interactive API compilation
test_info "Testing enhanced API compilation..."
if javac -cp ".:commons-io-2.11.0.jar" -d out/production/Archivit src/MainEnhanced.java 2>/dev/null; then
    test_passed "Enhanced MainEnhanced.java compiles successfully"
else
    test_failed "Enhanced MainEnhanced.java compilation failed"
fi

echo
echo "Phase 2: Non-Interactive API Tests"
echo "=================================="

# Test 3: Non-interactive archive creation
test_info "Testing non-interactive archive creation..."
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced create test_data/sample_dir test_basic_create 2>/dev/null >/dev/null; then
    if [ -f "test_basic_create.archivit" ]; then
        test_passed "Non-interactive archive creation works"
    else
        test_failed "Archive file not created"
    fi
else
    test_failed "Non-interactive archive creation failed"
fi

# Test 4: Non-interactive archive listing
test_info "Testing non-interactive archive listing..."
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced list test_basic_create.archivit 2>/dev/null | grep -q "Archive contains"; then
    test_passed "Non-interactive archive listing works"
else
    test_failed "Non-interactive archive listing failed"
fi

# Test 5: Non-interactive archive extraction
test_info "Testing non-interactive archive extraction..."
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced extract test_basic_create.archivit test_basic_extract 2>/dev/null >/dev/null; then
    if [ -d "test_basic_extract" ]; then
        test_passed "Non-interactive archive extraction works"
    else
        test_failed "Extraction directory not created"
    fi
else
    test_failed "Non-interactive archive extraction failed"
fi

echo
echo "Phase 3: Password Protection Tests"
echo "================================="

# Test 6: Password-protected archive creation
test_info "Testing password-protected archive creation..."
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced create test_data/sample_dir test_password_create password123 2>/dev/null >/dev/null; then
    if [ -f "test_password_create.archivit" ]; then
        test_passed "Password-protected archive creation works"
    else
        test_failed "Password-protected archive file not created"
    fi
else
    test_failed "Password-protected archive creation failed"
fi

# Test 7: Password-protected archive extraction with correct password
test_info "Testing password-protected archive extraction..."
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced extract test_password_create.archivit test_password_extract password123 2>/dev/null >/dev/null; then
    if [ -d "test_password_extract" ]; then
        test_passed "Password-protected archive extraction works"
    else
        test_failed "Password-protected extraction directory not created"
    fi
else
    test_failed "Password-protected archive extraction failed"
fi

# Test 8: Wrong password rejection
test_info "Testing wrong password rejection..."
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced extract test_password_create.archivit test_wrong_password wrongpass 2>&1 | grep -q -i "password\|incorrect\|error"; then
    test_passed "Wrong password correctly rejected"
else
    test_failed "Wrong password was not rejected properly"
fi

echo
echo "Phase 4: Data Integrity Tests"
echo "============================="

# Test 9: File content verification
test_info "Testing file content integrity..."
if diff -r test_data/sample_dir test_basic_extract/test_basic_create >/dev/null 2>&1; then
    test_passed "Basic archive file content matches original"
else
    test_failed "Basic archive file content mismatch"
fi

# Test 10: Password-protected file content verification
test_info "Testing encrypted file content integrity..."
if diff -r test_data/sample_dir test_password_extract/test_password_create >/dev/null 2>&1; then
    test_passed "Encrypted archive file content matches original"
else
    test_failed "Encrypted archive file content mismatch"
fi

echo
echo "Phase 5: Security and Error Handling Tests"
echo "=========================================="

# Test 11: Invalid source path handling
test_info "Testing invalid source path handling..."
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced create /nonexistent/path test_invalid 2>&1 | grep -q -i "error\|invalid\|not.*valid"; then
    test_passed "Invalid source path correctly handled"
else
    test_failed "Invalid source path not handled properly"
fi

# Test 12: Archive overwrite protection
test_info "Testing archive overwrite protection..."
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced create test_data/sample_dir test_basic_create 2>&1 | grep -q -i "exists\|overwrite\|error"; then
    test_passed "Archive overwrite protection works"
else
    test_failed "Archive overwrite protection failed"
fi

# Test 13: API Test Suite
test_info "Running comprehensive API test suite..."
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" APITest >/dev/null 2>&1; then
    test_passed "Comprehensive API test suite passes"
else
    test_failed "API test suite has failures"
fi

echo
echo "Phase 6: Performance and Features Tests"
echo "======================================="

# Test 14: Large file handling (create a larger test file)
test_info "Testing larger file handling..."
mkdir -p test_data/large_test
dd if=/dev/zero of=test_data/large_test/large_file.dat bs=1024 count=100 >/dev/null 2>&1
echo "This is a test file with some text content that might be compressible" > test_data/large_test/text_file.txt
for i in {1..100}; do
    echo "Line $i: This is repeated content that should compress well" >> test_data/large_test/text_file.txt
done

if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced create test_data/large_test test_large 2>/dev/null >/dev/null; then
    if [ -f "test_large.archivit" ]; then
        test_passed "Large file archiving works"
    else
        test_failed "Large file archive not created"
    fi
else
    test_failed "Large file archiving failed"
fi

# Test 15: Archive format validation
test_info "Testing archive format validation..."
echo "invalid archive content" > test_invalid.archivit
if java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced list test_invalid.archivit 2>&1 | grep -q -i "error\|invalid\|signature"; then
    test_passed "Invalid archive format correctly detected"
else
    test_failed "Invalid archive format not detected"
fi

echo
echo "Phase 7: Cross-Platform Compatibility"
echo "===================================="

# Test 16: File permissions preservation
test_info "Testing file permissions preservation..."
if [ -x "test_basic_extract/test_basic_create/script.sh" ]; then
    test_passed "Executable permissions preserved"
else
    test_failed "Executable permissions not preserved"
fi

echo
echo "Phase 8: Memory and Resource Management"
echo "======================================"

# Test 17: Memory usage with large archives
test_info "Testing memory efficiency..."
# This test runs in the background to avoid blocking if there are memory issues
timeout 30s java -Xmx64m -cp ".:out/production/Archivit:commons-io-2.11.0.jar" MainEnhanced create test_data/large_test test_memory_limit 2>/dev/null >/dev/null
if [ $? -eq 0 ] && [ -f "test_memory_limit.archivit" ]; then
    test_passed "Memory-constrained operation succeeds"
else
    test_failed "Memory-constrained operation failed (may be expected for very large files)"
fi

echo
echo "Results Summary"
echo "==============="

# Calculate statistics
if [ $total_tests -gt 0 ]; then
    success_rate=$((passed_tests * 100 / total_tests))
else
    success_rate=0
fi

echo "Total tests run: $total_tests"
echo "Tests passed: $passed_tests"
echo "Tests failed: $((total_tests - passed_tests))"
echo "Success rate: $success_rate%"

if [ $passed_tests -eq $total_tests ]; then
    echo -e "\n${GREEN}🎉 All tests passed! The Java Archiver improvements are working correctly.${NC}"
    exit_code=0
else
    echo -e "\n${YELLOW}⚠️  Some tests failed. Review the output above for details.${NC}"
    exit_code=1
fi

# Cleanup
echo
echo "Cleaning up test files..."
rm -f test_*.archivit test_invalid.archivit
rm -rf test_*_extract test_*_create test_wrong_password
rm -rf test_data/large_test

echo -e "\n${GREEN}Test suite completed.${NC}"
exit $exit_code