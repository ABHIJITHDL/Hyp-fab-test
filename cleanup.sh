# docker stop $(docker ps -aq) && docker rm $(docker ps -aq)

# docker rmi $(docker images | grep dev)

docker stop $(docker ps -q)

docker rm $(docker ps -aq --filter status=exited)