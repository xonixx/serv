# Develop a utility on GraalVM

## Problem definition

Periodically I need to share files over a local network, for example, with a project colleague.

There are a lot of solutions for this - Samba / FTP / scp. You can simply upload the file to a publicly accessible place like Google Drive, attach it to a task in Jira, or even send as email.

But all this is to some extent inflexible, sometimes it requires preliminary setup and has its limitations (for example, the maximum size of the attachment).

And you want something more lightweight and flexible.

I always admired Linux for letting me quickly build a practical solution using only built-in tools.

Say, I often solved the problem mentioned above using the system python with the following one-liner

```bash
$ python3 -mhttp.server
Serving HTTP on 0.0.0.0 port 8000 ...
```

This command starts the web server in the current folder and allows you to get a list of files through the web interface and download them. You can find more similar tricks here: https://gist.github.com/willurd/5720255.

There are several inconveniences though. Now to transfer the download link to your colleague, you need to know your network IP address.

For this you can just do:

```bash
$ ifconfig -a 
```
And then from the received list of network interfaces select the appropriate one and manually compile a link like http://IP:8000. Now send it to your colleague.

The second inconvenience: this server is single threaded. Thus while one of your colleagues is downloading the file the other one will not even be able to see the list of files.

Thirdly - it is inflexible. If you need to transfer only one file it will be unnecessary to open the entire folder i.e. you will have to perform such gestures (and don't forget to clean up the garbage afterwards):

```bash
$ mkdir tmp1
$ cp file.zip tmp1
$ cd tmp1
$ python3 -mhttp.server
```

The fourth inconvenience - there is no _simple_ way to download the entire contents of the folder.

To transfer the contents of the folder a technique called [tar pipe](https://docstore.mik.ua/orelly/unix3/upt/ch10_13.htm) is usually used.

Example:
```bash
$ ssh user@host 'cd /path/to/source && tar cf - .' | cd /path/to/destination && tar xvf -
```

No worry if it's not clear, I will explain how it works. The first part of the `tar cf - .` command compiles an archive of the contents of the current folder and writes to standard output. Then the output is transmitted through the pipe via a secure ssh channel to the input of a similar command `tar xvf -` which does the reverse procedure i.e. reads standard input and unzips to current folder. In fact this is a transfer of archived data but without creating an intermediate file!

Obviously this approach is "slightly" inconvenience. You need to have ssh access from one machine to another which generally is almost never set up.

Is it possible to achieve same functionality but without the problems described?

Finally let's formalize what we are going to build:
1. A program that is easy to install (static binary)
1. Able to transfer both the file and the folder with all contents
1. With optional compression
1. Which allows the receiving party to download the file(s) using only standard *nix tools (wget/curl/tar)
1. After launch the program immediately shows the exact commands for downloading.

## Solution

At the [JEEConf](https://jeeconf.com/) conference that I attended not long ago the topic of [Graal](https://www.graalvm.org/) was raised pretty often. The topic is far from new but finally I had a chance to play with it on my own.

For those who are not yet in context (do these people exist? oO) let me remind you that GraalVM is a JVM on steroids from Oracle with additional features. The most notable are:
1. Polyglot JVM - the ability to seamlessly launch Java, Javascript, Python, Ruby, R, etc. code
1. Support AOT compilation - compiling Java directly into a native binary
1. A less noticeable but very cool feature: the C2 compiler has been rewritten from C++ to Java for the purpose of more convenient further development. This has already produced noticeable results. The new compiler makes much more optimizations at the stage of converting Java bytecode to the native code. It is able to remove allocations more efficiently. Twitter has reduced CPU consumption by 11% simply by turning on this setting. At their scale it gave a noticeable saving of resources (and money).

You can refresh your knowledge of ​​Graal [in this article](https://chrisseaton.com/truffleruby/tenthings/).

For implementation we will use Java so for us the most relevant feature will be an AOT compilation.

Actually the result of the development is presented [in this Github repository](https://github.com/xonixx/serv).

An example of usage for transferring a single file:

```bash
$ serv '/path/to/report.pdf' 
To download the file please use one of the commands below: 

curl http://192.168.0.179:17777/dl > 'report.pdf'
wget -O- http://192.168.0.179:17777/dl > 'report.pdf'
curl http://192.168.0.179:17777/dl?z --compressed > 'report.pdf'
wget -O- http://192.168.0.179:17777/dl?z | gunzip > 'report.pdf'
```

An example of usage for transferring the contents of a folder (all files including nested!):

```bash
$ serv '/path/to/folder' 
To download the files please use one of the commands below. 
NB! All files will be placed into current folder!

curl http://192.168.0.179:17777/dl | tar -xvf -
wget -O- http://192.168.0.179:17777/dl | tar -xvf -
curl http://192.168.0.179:17777/dl?z | tar -xzvf -
wget -O- http://192.168.0.179:17777/dl?z | tar -xzvf -
```

Yes, that simple!

Please note that the program itself determines the correct IP address serving the files.

## Observations / Reflections

It is clear that one of the goals in creating the program was its compactness. And here is the result achieved:

```bash
$ du -hs `which serv`
2.4M	/usr/local/bin/serv 
```

Incredibly, the entire JVM along with the application code fits in just a few megabytes! Of course, everything is somewhat more complex, but more on that later.

In fact the Graal compiler produces a binary of slightly more than 7 megs. Additionally I decided [compress it with UPX](https://github.com/upx/upx).

This turned out to be a good idea since the launch time increase was rather small.

Uncompressed version:
```bash
$ time ./build/com.cmlteam.serv.serv -v
0.1

real    0m0.001s
user    0m0.001s
sys     0m0.000s
```

Compressed:
```bash
$ time ./build/serv -v
0.1

real    0m0.021s
user    0m0.021s
sys     0m0.000s
```

For you to compare here is the launch time "in the traditional way":
```bash
$ time java -cp "/home/xonix/proj/serv/target/classes:/home/xonix/.m2/repository/commons-cli/commons-cli/1.4/commons-cli-1.4.jar:/home/xonix/.m2/repository/org/apache/commons/commons-compress/1.18/commons-compress-1.18.jar" com.cmlteam.serv.Serv -v
0.1

real    0m0.040s
user    0m0.030s
sys     0m0.019s
```

As you can see it is two times slower than the UPX-version.

In general a short starting time is one of the strengths of GraalVM. This as well as the low memory consumption caused a significant enthusiasm around using this technology for microservices and serverless.

I tried to make the logic of the program as minimal as possible and use a minimum amount of libraries. Such an approach is always recommended but additionally in this case I had concerns that adding third-party maven dependencies would significantly increase the size of the resulting program file.

Therefore I didn't use third-party dependencies for Java web server. Instead I used the JDK implementation from the `com.sun.net.httpserver.*` package. In general using the `com.sun.*` package is considered as a bad practice. I found it acceptable in this case since I compile it into native code and this means the compatibility among the JVMs is not so important.

However my fears were completely in vain. For convenience I used two dependencies in the program.
1. `commons-cli` - to parse command line arguments
1. `commons-compress` - to generate a folder tarball and optional gzip compression

The file size increase was barely noticeable. I would assume that the Graal compiler is very smart so as not to put all the used jar-files into the executable but only the code that is actually used by the application.

Compiling into native code on Graal is performed by the [native-image](https://www.graalvm.org/docs/reference-manual/aot-compilation/) utility. It is worth mentioning that this process is resource intensive. Say, in my not very slow configuration with an Intel 7700K CPU on board, this process takes 19 seconds. Therefore, I recommend to run the program during development as usual (via java), and assemble the binary at the final stage.

## Findings

I think the experiment turned out to be very successful. During the development using the Graal toolkit I did not encounter any significant problems. Everything worked predictably and consistently. Although it will almost certainly not be so smooth if you try to do something more complicated, for example, [Spring Boot app](https://royvanrijn.com/blog/2018/09/part-2-native-microservice-in-graalvm/). Nevertheless, a number of platforms has already been presented that claim to support Graal natively. Among them are [Micronaut](https://micronaut.io/), [Microprofile](https://microprofile.io/), [Quarkus](https://quarkus.io/).

As for the further development of the project the [list of improvements](https://github.com/xonixx/serv/issues) is already planned for version 0.2. Also at the moment only building of the binary for Linux x64 is implemented. I hope that this will improve, especially since the Graal compiler supports MacOS and Windows. Unfortunately it does not yet support cross-compilation, which could really help here.

I hope that the presented utility will be useful at least to someone from the respected community. I would be glad twice if there are those who want to contribute [into the project](https://github.com/xonixx/serv).