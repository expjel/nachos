#include "syscall.h"
#include "stdio.h"
#include "strcmp.c"
#include "strlen.c"

int main() {
    char *expected = "this is just a test\nwoot woot\ntest a just is this\n\n\nyay!";

    printf("testing read one byte at a time\n");
    int fd = open("to_read.txt");
    char buf[128];
    int numRead = 0;
    int totalRead = 0;
    do {
        numRead = read(fd, buf+totalRead, 1);
        totalRead += numRead;
    } while (numRead > 0);
    if (totalRead != strlen(expected)) {
        printf("\tread() did not read the correct number of bytes; expected %d, got %d\n", strlen(expected), totalRead);
        exit(1);
    }
    if (strcmp(buf, expected) != 0) {
        printf("\tread() did not read the correct bytes; expected %s, got %s\n", expected, buf);
        exit(1);
    }
    printf("passed!\n");

    printf("testing read all at once\n");
    int fd2 = open("to_read.txt");
    totalRead = read(fd2, buf, 128);
    if (totalRead != strlen(expected)) {
        printf("\tread() did not read the correct number of bytes; expected %d, got %d\n", strlen(expected), totalRead);
        exit(1);
    }
    if (strcmp(buf, expected) != 0) {
        printf("\tread() did not read the correct bytes; expected %s, got %s\n", expected, buf);
        exit(1);
    }
    printf("passed!\n");

    printf("testing read all at once with very large count\n");
    int fd3 = open("to_read.txt");
    totalRead = read(fd3, buf, 1024 * 100);
    if (totalRead != strlen(expected)) {
        printf("\tread() did not read the correct number of bytes; expected %d, got %d\n", strlen(expected), totalRead);
        exit(1);
    }
    if (strcmp(buf, expected) != 0) {
        printf("\tread() did not read the correct bytes; expected %s, got %s\n", expected, buf);
        exit(1);
    }
    printf("passed!\n");
    
    int res = close(fd);
    int res2 = close(fd2);
    int res3 = close(fd3);
    if (res != 0 || res2 != 0 || res3 != 0) {
        printf("close() failed; expected 0 and 0, got %d and %d and %d\n", res, res2, res3);
        exit(1);
    }

    // error cases:
    printf("read() should error out with invalid file descriptor\n");
    int r = read(-1, buf, 10);
    if (r != -1) {
        printf("\tsomehow read successfully with fd = -1; r=%d\n", r);
        exit(1);
    }
    r = read(5, buf, 10);
    if (r != -1) {
        printf("\tsomehow read successfully with fd = 5; r=%d\n", r);
        exit(1);
    }
    r = read(5, buf, 0); // even with count=0, should error out
    if (r != -1) {
        printf("\tsomehow read successfully with fd = 5; r=%d\n", r);
        exit(1);
    }
    printf("passed!\n");
    
    printf("read() should error out with invalid buffer pointer\n");
    int fd4 = open("to_read.txt");
    r = read(fd4, 0xBADFFF, 128);
    if (r != -1) {
        printf("\tsomehow read successfully with invalid buffer address\n");
        exit(1);
    }
    
    // this is testing that you cannot read into read-only memory
    fd4 = open("to_read.txt");
    r = read(fd4, (char *)0, 128);
    if (r != -1) {
        printf("\tsomehow read successfully with invalid buffer address\n");
        exit(1);
    }
    printf("passed!\n");

    printf("read() should error out with invalid count\n");
    r = read(fd4, buf, -1);
    if (r != -1) {
        printf("\tsomehow read successfully with invalid count\n");
        exit(1);
    }
    printf("passed!\n");

    // we should now also test what the expected behavior is if you read 0 bytes from something
    // we will use creat() to guarantee an empty file.
    int fd5 = creat("empty.txt");
    if (fd5 == -1) {
        printf("failed to create\n");
        exit(1);
    }
    
    printf("read() should ignore bad buffer if there is nothing to read\n");
    r = read(fd5, 0xBADFFF, 128); // https://piazza.com/class/kt9mtpbh4g01h9?cid=566
    if (r != 0) {
        printf("read() did not ignore the bad buffer\n");
    }
    printf("passed!\n");
}
