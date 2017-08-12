## Welcome to Blazegraph

Blazegraph™ is our ultra high-performance graph database supporting Blueprints and RDF/SPARQL APIs. It supports up to 50 Billion edges on a single machine and has a High Availability and Scale-out architecture. It is in production use for Fortune 500 customers such as EMC, Autodesk, and many others.  It powers the Wikimedia Foundation's Wiki Data Query Service.  See the latest [Feature Matrix](http://www.blazegraph.com/blazegraph#FeatureMatrix).

![image](http://www.blazegraph.com/static/images/blazegraph_by_systap.png)

Please see the release notes in [bigdata/src/releases](bigdata/src/releases) for getting started links.

[Sign up](http://eepurl.com/VLpUj) to get the latest news on Blazegraph.

Please also visit us at our: [website](http://www.blazegraph.com), [wiki](https://wiki.blazegraph.com), and [blog](https://wiki.blazegraph.com/).

Find an issue?   Having a problem?  See [JIRA](https://jira.blazegraph.com) or purchase [Support](https://www.blazegraph.com/buy).



 
 ----------------------------------------------------

**Quick setup ( using  [Docker Hub image](https://hub.docker.com/r/rac021/blz_cluster_2_nodes) ) no need scripts ; change IPs and ports if needed :**
 
 ```
❯    docker network create --subnet=192.168.56.250/24 mynet123

     docker run -d --net mynet123 --name blz_host_0                   \
                --memory-swappiness=0	                             \
                --add-host blz_host_0:192.168.56.10                   \
                --add-host blz_host_1:192.168.56.20                   \
                --add-host blz_host_2:192.168.56.30                   \
                --ip 192.168.56.10                                    \
                -it --entrypoint /bin/bash rac021/blz_cluster_2_nodes \
                -c "./bigdata start; while true; do sleep 1000; done  "
                   
     docker run -d --net mynet123 --name blz_host_1                   \
                --memory-swappiness=0	                             \
                --add-host blz_host_0:192.168.56.10                   \
                --add-host blz_host_1:192.168.56.20                   \
                --add-host blz_host_2:192.168.56.30                   \
                --ip 192.168.56.20                                    \
                -it --entrypoint /bin/bash rac021/blz_cluster_2_nodes \
                -c "./bigdata start; while true; do sleep 1000; done  "

     docker run -d --net mynet123 --name blz_host_2                   \
                --memory-swappiness=0	                             \
                --add-host blz_host_0:192.168.56.10                   \
                --add-host blz_host_1:192.168.56.20                   \
                --add-host blz_host_2:192.168.56.30                   \
                --ip 192.168.56.30                                    \
                -it --entrypoint /bin/bash rac021/blz_cluster_2_nodes \
                -c "./bigdata start; while true; do sleep 1000; done  "
        
 ------------------------------------------------------------------------------------------

     docker run -d --net mynet123                                                         \
                -l traefik.backend=client_blz_backend                                     \
                -l traefik.frontend.rule=Host:client.blz.localhost                        \
                --name  cl_01_blz                                                         \
                --ip    192.168.56.200                                                    \
                -p      9990:9999                                                         \
                --expose 9999                                                             \
                --memory-swappiness=0                                                     \
                --entrypoint /bin/bash -it rac021/blz_cluster_2_nodes                     \
                -c " ./nanoSparqlServer.sh 9999 ola rw ;  while true; do sleep 1000; done "
                           
     docker run -d --net mynet123                                                         \
                -l traefik.backend=client_blz_backend                                     \
                -l traefik.frontend.rule=Host:client.blz.localhost                        \
                --name  cl_02_blz                                                         \
                --ip    192.168.56.210                                                    \
                -p      9995:9999                                                         \
                --memory-swappiness=0                                                     \
                --entrypoint /bin/bash -it rac021/blz_cluster_2_nodes                     \
                -c " ./nanoSparqlServer.sh 9999 ola rw ;  while true; do sleep 1000; done "

 ------------------------------------------------------------------------------------------
 
 docker exec cl_01_blz tail -Fn 30 ---disable-inotify --retry -s 1  /nas/bigdata/benchmark/log/error.log
 docker exec cl_01_blz tail -Fn 30 ---disable-inotify --retry -s 1 /nas/bigdata/benchmark/log/detail.log
 docker exec cl_01_blz tail -Fn 30 ---disable-inotify --retry -s 1  /nas/bigdata/benchmark/log/event.log
     
 docker exec cl_02_blz tail -Fn 30 ---disable-inotify --retry -s 1  /nas/bigdata/benchmark/log/error.log
 docker exec cl_02_blz tail -Fn 30 ---disable-inotify --retry -s 1 /nas/bigdata/benchmark/log/detail.log
 docker exec cl_02_blz tail -Fn 30 ---disable-inotify --retry -s 1  /nas/bigdata/benchmark/log/event.log
   
```
 
----------------------------------------------------

