# serv

The simple tool to quickly share file or folder in local network.

## Usage

Help message:
```
$ ./serv
Provide at least 1 file/folder to serve
usage: serv
 -C,--compress     enable compression (default = false)
 -p,--port <arg>   port to serve on (default = 17777)
```

Sharing a file:
```bash
$ ./serv ~/path/to/report.pdf 
To download the file please use: 

curl http://192.168.0.179:17777/dl > 'report.pdf'
 -or-
wget -O- http://192.168.0.179:17777/dl > 'report.pdf'
```

Sharing a folder (all the files in it):
```bash
$ ./serv ~/path/to/folder/
To download the files please use commands below. 
NB! All files will be placed into current folder!

curl http://192.168.0.179:17777/dl | tar -xvf -
 -or-
wget -O- http://192.168.0.179:17777/dl | tar -xvf -
```

*Yes! All that simple!*