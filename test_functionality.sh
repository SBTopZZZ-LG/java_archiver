#!/bin/bash

# Non-interactive test for Java Archiver
cd /home/runner/work/java_archiver/java_archiver

echo "Testing Java Archiver..."

# Test 1: Create archive without password
echo "=== Test 1: Creating archive without password ==="
echo -e "1\n/home/runner/work/java_archiver/java_archiver/test_data/sample_dir/\n/home/runner/work/java_archiver/java_archiver/test_output.archivit\nn" | java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" Main

if [ -f "test_output.archivit" ]; then
    echo "✓ Archive created successfully"
    ls -la test_output.archivit
else
    echo "✗ Archive creation failed"
    exit 1
fi

# Test 2: List archive contents
echo -e "\n=== Test 2: Listing archive contents ==="
echo -e "3\ntest_output.archivit" | java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" Main

# Test 3: Extract archive
echo -e "\n=== Test 3: Extracting archive ==="
mkdir -p extract_test
echo -e "2\ntest_output.archivit\n/home/runner/work/java_archiver/java_archiver/extract_test/" | java -cp ".:out/production/Archivit:commons-io-2.11.0.jar" Main

if [ -d "extract_test/test_output" ]; then
    echo "✓ Archive extracted successfully"
    find extract_test -type f -exec echo "  {}" \;
else
    echo "✗ Archive extraction failed"
    exit 1
fi

# Test 4: Compare original and extracted files
echo -e "\n=== Test 4: Comparing files ==="
if diff -r test_data/sample_dir extract_test/test_output; then
    echo "✓ Files match perfectly"
else
    echo "✗ File content mismatch"
    exit 1
fi

echo -e "\n=== All tests passed! ==="