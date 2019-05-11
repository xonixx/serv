# serv

The simple tool to quickly share file or folder in local network.

After running the tool it gives you the precise commands that you can pass to your teammate to receive the file(s).

## Usage

Sharing a file:
```bash
$ serv '/path/to/report.pdf' 
To download the file please use one of the commands below: 

curl http://192.168.0.179:17777/dl > 'report.pdf'
wget -O- http://192.168.0.179:17777/dl > 'report.pdf'
curl http://192.168.0.179:17777/dl?z --compressed > 'report.pdf'
wget -O- http://192.168.0.179:17777/dl?z | gunzip > 'report.pdf'
```

Sharing a folder (all the files in it):
```bash
$ serv '/path/to/folder' 
To download the files please use one of the commands below. 
NB! All files will be placed into current folder!

curl http://192.168.0.179:17777/dl | tar -xvf -
wget -O- http://192.168.0.179:17777/dl | tar -xvf -
curl http://192.168.0.179:17777/dl?z | tar -xzvf -
wget -O- http://192.168.0.179:17777/dl?z | tar -xzvf -
```

Help message:
```
$ serv -h
serv ver. 0.1
usage: serv [...options] <file or folder>
 -h,--help         print help and exit
 -p,--port <arg>   port to serve on (default = 17777)
 -v,--version      show version and exit
```

*Yes! All that simple!*