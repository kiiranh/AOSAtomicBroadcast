/*
* CS 6378.002: Advanced Operating Systems
* Fall 2013 | Project 1
* Kiran Gavali
*/

*************
* COMPILING *
*************

1. Unzip the contents of the archive: AOS_project1_KiranGavali_final.zip
   It will create the following directories/files:
   AOS/src: contains the source files
   AOS/bin: contains the binaries
   AOS/aos_project1_kxg121530.sh: Script to clear previous binaries and compile again
2. Execute the following commands:
   cd AOS/
   chmod +x aos_project1_kxg121530.sh
   ./aos_project1_kxg121530.sh
3. At this point, the new binaries should be placed in the bin folder.

***********************
* RUNNING THE PROGRAM *
***********************

1. Make sure you are in the unzipped AOS directory.
2. The default configuration file is in the AOS/src folder: AOS/src/config.txt
3. Make required modifications to the configuration file.
4. cd to the bin folder: cd bin.
5. From the bin directory execute the following command:
   java startup.Node <NodeID> <Absolute Path of config file> <Absolute path of directory where log is to be placed>
   Eg: java startup.Node 0 /home/kiiranh/AOS/src/config.txt /home/kiiranh/AOS/bin/
6. This will start the program on the system. 
7. The program has to be started on all the nodes mentioned in the config file. Only when all nodes are up
   the nodes will make connections and start execution.

**********
* RESULT *
**********

1. The program progress and results will be displayed on the terminal.
2. The delivered messages will be written to a file (node<id>.log) in the specified log directory.
   Each node will write to its separate file, eg: Node 0 will write to node0.log
3. These files can be used to verify the message order manually using the Linux cmp command.

*************************
* AUTOMATED VERIFCATION *
*************************

1. At the beginning, a Leader node is elected (the first in the list in config file)
2. Leader broadcasts a START signal to all nodes to start computation.
3. When the application on all nodes are done processing, they will send the Result to Leader Node Application.
4. The application on the leader node will verify that the results of all nodes are consisten (same).
5. When application work is done, it will stop.
6. Leader will broadcast STOP signal to all nodes, on receiving which the Service and the node itself will stop.
7. The Leader Node will stop is application and service and then perform verification of the log files.
8. Log file verification: Compute MD5 checksum of all log files and check if they are consistent. 

