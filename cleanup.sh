#docker stop $(docker ps -aq) && docker rm $(docker ps -aq)

#docker rmi $(docker images | grep dev)

docker stop $(docker ps -a -q) && docker rm $(docker ps -a -q)