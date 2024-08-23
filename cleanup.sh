docker stop $(docker ps -aq) && docker rm $(docker ps -aq)

docker rmi $(docker images | grep dev)

#!/bin/bash

# # Stop and remove all Docker containers
# docker stop $(docker ps -a -q) && docker rm $(docker ps -a -q)

# # Remove all Docker networks
# docker network prune -f

# # Remove all Docker volumes
# docker volume prune -f

# # Remove any existing channel artifacts
# rm -rf ./channel-artifacts/*