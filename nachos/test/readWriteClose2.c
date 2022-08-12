/**
 * readWriteClose2.c
 * test read, write, close
 * Depends on open() and creat() already working
 */
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(){
	int fd, status, count;
	char buf[256];
	int fds[4];
	
	/*
	printf("\n----- Test 1: attempt to read more than buffer than hold -----\n");
	fd = open("READ.txt");
	count = read(fd, buf, 4096);
	if (count > 256)
		printf("Error: more bytes read than possible: %d\n", count);
	else
		printf("Test 1 passed: %d bytes read\n", count);
	
	printf("\n----- Test 2: attempt to read more than buffer than hold (2) -----\n");
	fd = open("READ.txt");
	count = read(fd, buf, 257);
	if (count > 256)
		printf("Error: more bytes read than possible: %d\n", count);
	else
		printf("Test 2 passed: %d bytes read\n", count);
	*/
	
	printf("\n----- Test 3: attempt to read negative bytes -----\n");
	fd = open("READ.txt");
	count = read(fd, buf, -2048);
	if (count != -1)
		printf("Error: negative bytes were read????\n");
	else
		printf("Test 3 passed\n");
	
	printf("\n----- Test 4: attempt to read more bytes than file -----\n");
	fd = open("SHORT_READ.txt");
	//contents: "hello\n\0"
	count = read(fd, buf, 256);
	if (count != 7)
		printf("Error: more bytes than existed were read.\n");
	else
		printf("Test 4 passed\n");
	
	printf("\n----- Test 5: read to NULL -----\n");
	fd = open("READ.txt");
	count = read(fd, NULL, 32);
	if (count != -1)
		printf("Error: read to invalid buffer\n");
	else
		printf("Test 5 passed\n");
	
	printf("\n----- Test 6: attempt to write negative bytes -----\n");
	fd = creat("test.txt");
	count = write(fd, buf, -2048);
	if (count != -1)
		printf("Error: negative bytes were written????\n");
	else
		printf("Test 6 passed\n");
	
	printf("\n----- Test 7: write from NULL -----\n");
	fd = creat("test.txt");
	count = write(fd, NULL, 32);
	if (count != -1)
		printf("Error: wrote from invalid buffer\n");
	else
		printf("Test 7 passed\n");
	
	printf("\n----- Test 8: attempt to read more than buffer than hold (8) -----\n");
	fd = open("READ.txt");
	count = read(fd, buf, 512);
	if (count > 256)
		printf("Error: more bytes read than possible: %d\n", count);
	else
		printf("Test 2 passed: %d bytes read\n", count);
	
	printf("\n----- Test 9: write from ridiculouly high address -----\n");
	fd = creat("test.txt");
	count = write(fd, (char*) 0xFFFFFF00, 32);
	if (count != -1)
		printf("Error: wrote from invalid buffer\n");
	else
		printf("Test 9 passed\n");
	
	printf("\n----- Test 10: read to ridiculouly high address -----\n");
	fd = open("READ.txt");
	count = read(fd, (char*) 0xFFFFFF00, 32);
	if (count != -1)
		printf("Error: read to invalid buffer\n");
	else
		printf("Test 10 passed\n");
	
	return 0;
}
