2026/05/07 22:08 

Today I set up my Project 3 folder but because I'm outside and using my friend's laptop now, so I haven't set up the git repository yet. Based on the instructions, this project is an index file program using a B-Tree, and I am planning on using Java to complete this project. The program needs to support commands like create, insert, search, load, print, and extract. The index file is divided into 512-byte blocks, with the first block used as the header and the rest used for B-Tree nodes. Each node stores keys, values, and child pointers, and the B-Tree has minimum degree 10, so each node can hold up to 19 key/value pairs and 20 child pointers. The main challenge is making sure the binary file format is correct and that the B-Tree insert/search logic works without loading too many nodes into memory at the same time. 

Overall plan: I want to finish this project in two day sessions. On Day 1, I will set up the basic Java program structure, handle command-line arguments, create the helper methods for each command, and implement the create command with the correct file header. Then I will create the B-Tree node structure and write the methods to read and write nodes from the index file. If there is enough time, I will start the basic search command and test it with a small manually created tree. On Day 2, I will finish the search command, implement insert for empty trees and non-full nodes, and then add the split logic for full nodes. After the main B-Tree logic works, I will add load, print, and extract. Finally, I will test the program with valid and invalid commands, make sure existing files are not overwritten, clean up the output, and write the README/devlog before submission.


2026/05/07 22:17

a. Thoughts so far:
    same as initial commit

b. Plan for this session (Day 1):
    Goal: build the basic structure and get the file/search foundation working
    1. Set up the main Java file and command-line handling
    2. Add the basic command structure for all required commands
    3. Implement create with the correct index file header
    4. Add basic file validation
    5. Create the B-Tree node read/write helpers
    6. Start and test the search command


2026/05/07 23:24 -- Finished the basic structure and command parsing


2026/05/08 00:51 -- Created command and header for the program, tested it with command "java Project3 create test.idx", and get the output of "Index file created."


2026/05/08 01:29 -- Got home and start setting up the git repository


2026/05/08 03:11 -- Added the BTreeNode class and helper methods for reading and writing node blocks


2026/05/08 04:16 -- Added the search command and file validation. Since insert is not implemented yet, when tested search on an empty index file and got "Error: key not found."


2026/05/08 13:02

a. Thoughts so far:
    Day 1 finished the basic foundation of the project. The program can now handle command-line arguments, recognize all required commands, create a new index file with the correct header, validate an index file, and has helper methods for reading and writing B-Tree node blocks. I also added a basic search command, and tested it on an empty index file. The next main challenge is implementing insertion, especially when nodes become full and need to split.

b. Plan for this session (Day 2):
    Goal: finish the main B-Tree operations and complete the remaining commands
    1. Implement insert for an empty tree
    2. Implement insert for a leaf node that still has space
    3. Add split logic for full nodes
    4. Test insert and search together with several key/value pairs
    5. Implement load, print, and extract
    6. Test invalid inputs, missing files, and existing output files
    7. Clean up the code, update the devlog, and prepare the README


2026/05/08 14:13 -- Added basic insert for an empty tree and for a non-full leaf node


2026/05/08 15:34 -- While adding root split logic, found a bug where the median key was copied into the right child while also being promoted to the new root.


2026/05/08 16:02 -- Fixed the root split bug. The issue was that the right node copied from the median index, which left the median key in both the parent and the right child.


2026/05/08 17:20 -- Finished insert with spliting


2026/05/08 18:42 -- Added load, print, and extract commands. The print and extract commands use in-order traversal so the key/value pairs come out sorted by key.


2026/05/08 20:15 -- Finalized the code, and add the README file, ready to submit