# Develop a utility on GraalVM

## Formulation of the problem

Periodically, I have the task to share files over a local network, for example, with a project colleague.

There can be a lot of solutions for this - Samba / FTP / scp. You can simply upload the file to a publicly accessible public place like Google Drive, attach it to a task in Jira, or even send a letter.

But all this is to some extent inflexible, somewhere it requires preliminary adjustment and has its limitations (for example, the maximum size of the attachment).

And you want something more lightweight and flexible.

I was always pleasantly surprised by the opportunity in Linux, using available tools, to quickly build a practical solution.

Say, I often solved the above mentioned problem using the system python with the following one-liner

``` bash
$ python3 -mhttp.server
Serving HTTP on 0.0.0.0 port 8000 ...
```

This command starts the web server in the current folder and allows you to get a list of files through the web interface and download them. More similar items can be poured here: https://gist.github.com/willurd/5720255.

There are several inconveniences. Now to transfer the download link to your colleague, you need to know your IP address on the network.

For this it is convenient to use the command

``` bash
$ ifconfig -a
```
And then from the received list of network interfaces, select the appropriate one and manually compile a link like http: // IP: 8000, which you should send.

The second inconvenience: this server is single threaded. This means that while one of your colleagues is downloading the file, the second one will not even be able to download the list of files.

Thirdly - it is inflexible. If you need to transfer only one file, it will be unnecessary to open the entire folder, i.e. will have to perform such gestures (and after still cleaning the garbage):

``` bash
$ mkdir tmp1
$ cp file.zip tmp1
$ cd tmp1
$ python3 -mhttp.server
```

The fourth inconvenience - there is no _simple_ way to download the entire contents of the folder.

To transfer the contents of the folder is usually used a technique called [tar pipe](https://docstore.mik.ua/orelly/unix3/upt/ch10_13.htm).

Do something like this:
``` bash
$ ssh user @ host 'cd / path / to / source && tar cf -.' | cd / path / to / destination && tar xvf -
```

If suddenly it is not clear, I will explain how it works. The first part of the `tar cf - .` command will compile an archive of the contents of the current folder and write to standard output. Then this output through the pipe is transmitted via a secure ssh channel to the input of a similar command `tar xvf -` which does the reverse procedure, i.e. reads standard input and unzips to current folder. In fact, there is a transfer of archive files, but without creating an intermediate file!

Obviously, the inconvenience of this approach. You need ssh access from one machine to another, which generally almost never does.

Is it possible to achieve all of the above, but without these problems described?

So, it's time to formalize what we will build:
1. A program that is easy to install (static binary)
1. Which will allow to transfer both the file and the folder with all contents
1. With optional compression
1. Which will allow the receiving party to download the file (s) using only standard * nix tools (wget / curl / tar)
1. After launch, the program will immediately issue exact commands for downloading.

## Decision

At the [JEEConf](https://jeeconf.com/) conference , which I attended not so long ago, the [Graal](https://www.graalvm.org/) topic was raised repeatedly. The topic is far from new, but for me it was a trigger to finally touch this beast with my own hand.

For those who are not yet in the subject line (are there really any such? OO) let me remind you that GraalVM is such a pumped-up JVM from Oracle with additional features, the most notable of which are:
1. Polyglot JVM - the ability to seamlessly launch Java, Javascript, Python, Ruby, R, etc. code
1. Support AOT compilation - compiling Java directly into a native binary
1. A less noticeable, but very cool feature - the C2 compiler has been rewritten from C++ to Java for the purpose of more convenient further development. This has already produced noticeable results. This compiler makes much more optimizations at the stage of converting Java bytecode to native code. For example, it is able to more effectively remove allocations. Twitter was able to reduce CPU consumption by 11% simply by turning on this setting, which in their scale gave a noticeable saving of resources (and money).

You can refresh the idea of ​​Graal, for example, [in this habr-article](https://habr.com/ru/company/haulmont/blog/433432/).

We will write in Java, so for us the most relevant feature will be an AOT compilation.

Actually, the result of the development is presented [in this Github repository](https://github.com/xonixx/serv).

An example of using to transfer a single file:

``` bash
$ serv '/path/to/report.pdf'
To download the file please use one of the commands below:

curl http://192.168.0.179:17777/dl> 'report.pdf'
wget -O- http://192.168.0.179:17777/dl> 'report.pdf'
curl http://192.168.0.179:17777/dl?z --compressed> 'report.pdf'
wget -O- http://192.168.0.179:17777/dl?z | gunzip> 'report.pdf'
```

An example of use when transferring the contents of a folder (all files including nested!):

``` bash
$ serv '/ path / to / folder'
To download the files please use one of the commands below.
NB! All files will be placed into the current folder!

curl http://192.168.0.179:17777/dl | tar -xvf -
wget -O- http://192.168.0.179:17777/dl | tar -xvf -
curl http://192.168.0.179:17777/dl?z | tar -xzvf -
wget -O- http://192.168.0.179:17777/dl?z | tar -xzvf -
```

Yes, that simple!

Please note - the program itself determines the correct IP address on which files will be available for download.

## Observations / Reflections

It is clear that one of the goals in creating the program was its compactness. And here is the result achieved:

``` bash
$ du -hs `which serv`
2.4M / usr / local / bin / serv
```

Incredibly, the entire JVM, along with the application code, fit in a measly few megabytes! Of course, everything is somewhat wrong, but more on that later.

In fact, the Graal compiler produces a binary of slightly more than 7 megabytes. I decided to additionally [compress it with UPX](https://github.com/upx/upx).

This turned out to be a good idea, since the launch time was very inconsequential at the same time:

Uncompressed version:
``` bash
$ time ./build/com.cmlteam.serv.serv -v
0.1

real 0m0.001s
user 0m0.001s
sys 0m0.000s
```

Compressed:
``` bash
$ time ./build/serv -v
0.1

real 0m0.021s
user 0m0.021s
sys 0m0.000s
```

For comparison, the launch time "in the traditional way":
``` bash
$ time java -cp "/home/xonix/proj/serv/target/classes:/home/xonix/.m2/repository/commons-cli/commons-cli/1.4/commons-cli-1.4.jar:/home/ xonix / .m2 / repository / org / apache / commons / commons-compress / 1.18 / commons-compress-1.18.jar "com.cmlteam.serv.Serv -v
0.1

real 0m0.040s
user 0m0.030s
sys 0m0.019s
```

As you can see, two times slower than the UPX-version.

In general, a short start time is one of the strengths of GraalVM. This, as well as the low memory consumption, caused a significant enthusiasm around using this technology for microservices and serverless.

I tried to make the logic of the program as minimal as possible and use a minimum of libraries. In principle, such an approach is generally justified, and in this case I had concerns that adding third-party maven dependencies would significantly “weight down” the resulting program file.

For example, therefore, I did not use third-party dependencies for a Java web server (there are a lot of them for every taste and color), but I used the JDK implementation of a web server from the `com.sun.net.httpserver. *` Package. In general, using the `com.sun. *` Package is considered a moveton, but I found it to be valid in this case, since I compile it into native code, and this means there is no question of compatibility between the JVM.

However, my fears were completely in vain. In the program, I used two dependencies for convenience.
1. `commons-cli` - for parsing command line arguments
1. `commons-compress` - to generate a folder tarball and optional gzip compression

At the same time, the file size has increased very slightly. I would venture to suggest that the Graal compiler is very smart so as not to put all the connected jar-nicks into the executable file, but only the code of them that is actually used by the application code.

Compiling into native code on Graal is performed by the [native-image](https://www.graalvm.org/docs/reference-manual/aot-compilation/) utility. It is worth mentioning that this process is resource intensive. Say, in my not very slow configuration with an Intel 7700K CPU on board, this process takes 19 seconds. Therefore, I recommend to start the program in development as usual (via java), and collect the binary at the final stage.

## Findings

The experiment, I think, turned out to be very successful. When developing, using the Graal toolkit, I did not encounter any insurmountable or even significant problems. Everything worked predictably and consistently. Although it will almost certainly not be so smooth if you try to put together something more complicated, for example, [Spring Boot app](https://royvanrijn.com/blog/2018/09/part-2-native-microservice-in-graalvm/). Nevertheless, a number of platforms in which native support for Graal is announced has already been presented. Among them are [Micronaut](https://micronaut.io/), [Microprofile](https://microprofile.io/), [Quarkus](https://quarkus.io/).

As for the further development of the project, the [list of improvements](https://github.com/xonixx/serv/issues), already planned for version 0.2, is already ready. Also, the build of the final binaries for Linux x64 only is implemented. I hope that this omission will be corrected in the future, especially since the `native-image` compiler from Graal supports MacOS and Windows. Unfortunately, he does not yet support cross-compilation, which would greatly facilitate the work.

I hope that the presented utility will be useful at least to someone from the respected Habra community. I would be glad twice if there are those who want to attribute [to the project](https://github.com/xonixx/serv).