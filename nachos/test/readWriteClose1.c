/**
 * readWriteClose1.c
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
	
	printf("\n----- Test 1: close on negative fd -----\n");
	fd = -1;
	status = close(fd);
	if (status != -1){
		printf("Error: expected close to fail with fd -1, received status %d\n", status);
	} else
		printf("Test 1 passed\n");
	
	printf("\n----- Test 2: close on fd > 15 -----\n");
	fd = 16;
	status = close(fd);
	if (status != -1){
		printf("Error: expected close to fail with fd 16, received status %d\n", status);
	} else
		printf("Test 2 passed\n");
	
	printf("\n----- Test 3: close on fd that was never used before -----\n");
	fd = 4;
	status = close(fd);
	if (status != -1){
		printf("Error: expected close to fail with fd 4, received status %d\n", status);
	} else
		printf("Test 3 passed\n");
	
	printf("\n----- Test 4: close on valid opened fd -----\n");
	fd = creat("test.txt");
	status = close(fd);
	if (status != 0){
		printf("Error: expected close to succeed with fd %d, received status %d\n", fd, status);
	} else
		printf("Test 4 passed\n");
	
	printf("\n----- Test 5: close on fd that was already closed -----\n");
	fd = creat("test.txt");
	status = close(fd);
	status = close(fd);
	if (status != -1){
		printf("Error: expected close to fail with fd %d, received status %d\n", fd, status);
	} else
		printf("Test 5 passed\n");
	
	printf("\n----- Test 6: cannot write to a file that is closed -----\n");
	fd = creat("test6.txt");
	status = close(fd);
	if (write(fd, "Error: can write to file even though it's closed\n", 60) > 0)
		printf("Error: write succeeded even though file was closed\n");
	fd = open("test6.txt");
	count = read(fd, buf, 60);
	for (int i = 0; i < count; i++)
		putchar(buf[i]);
	
	printf("If nothing was printed, then Test 6 passed\n");
	
	printf("\n----- Test 7: read on negative fd -----\n");
	fd = -1;
	count = read(fd, buf, 32);
	if (count != -1){
		printf("Error: expected read to fail with fd -1, instead read %d bytes\n", status);
	} else
		printf("Test 7 passed\n");
	
	printf("\n----- Test 8: write on negative fd -----\n");
	fd = -1;
	count = write(fd, buf, 32);
	if (count != -1){
		printf("Error: expected write to fail with fd -1, instead read %d bytes\n", status);
	} else
		printf("Test 8 passed\n");
	
	printf("\n----- Test 9: read on fd > 15 -----\n");
	fd = 16;
	count = read(fd, buf, 32);
	if (count != -1){
		printf("Error: expected read to fail with fd 16, instead read %d bytes\n", status);
	} else
		printf("Test 9 passed\n");
	
	printf("\n----- Test 10: write on fd > 15 -----\n");
	fd = 16;
	count = write(fd, buf, 32);
	if (count != -1){
		printf("Error: expected write to fail with fd 16, instead read %d bytes\n", status);
	} else
		printf("Test 10 passed\n");
	
	printf("\n----- Test 11: reading same file from multiple fds (depends on write) -----\n");
	fd = creat("test.txt");
	write(fd, "This line should print 4 times!\n", 33);
	close(fd);
	for (int i = 0; i < 4; i++)
		fds[i] = open("test.txt");
	for (int i = 0; i < 4; i++){
		count = read(fds[i], buf, 32);
		if (count > 33)
			printf("Error: read() read too many characters\n");
		for (int j = 0; j < count; j++)
			putchar(buf[j]);
	}
	printf("If the line was printed four times, Test 11 passed\n");
	
	printf("\n----- Test 12: writing to same file from multiple fds (depends on read) -----\n");
	fds[0] = creat("test.txt");
	for (int i = 1; i < 4; i++)
		fds[i] = open("test.txt");
	
	write(fds[0], "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxand so do you\n\n", 64);
	write(fds[1], "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxI love debugging\n", 48);
	write(fds[2], "xxxxxxxxxxxxxxviolets are blue\n", 31);
	write(fds[3], "Roses are red\n", 14);
	
	for (int i = 0; i < 4; i++)
		close(fds[i]);
	
	fd = open("test.txt");
	count = read(fd, buf, 64);
	for (int j = 0; j < count; j++)
		putchar(buf[j]);
	printf("If a poem was printed, Test 12 passed\n");
	
	return 0;
}
