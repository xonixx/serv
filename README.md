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
 -H,--host <arg>    host to serve on (default is determined automatically)
 -h,--help          print help and exit
    --include-vcs   include VCS files (default = false)
 -p,--port <arg>    port to serve on (default = 17777)
 -v,--version       print version and exit
```

*Yes! All that simple!*

## Install

Sorry, but only Linux x64 is supported at the moment. Hopefully this will improve.

To install the tool simply run the command below.

```
$ sudo bash -c "
wget https://github.com/xonixx/serv/releases/download/v0.1/serv-linux-amd-64.executable -O/usr/local/bin/serv
chmod +x /usr/local/bin/serv 
"
```

Also since the tool is written in Java it can be run in any environment with Java 8 or above.