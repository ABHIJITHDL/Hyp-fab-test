export CORE_PEER_TLS_ENABLED=true
export ORDERER_CA=${PWD}/artifacts/channel/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem
export PEER0_ORG1_CA=${PWD}/artifacts/channel/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
export PEER0_ORG2_CA=${PWD}/artifacts/channel/crypto-config/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt
export FABRIC_CFG_PATH=${PWD}/artifacts/channel/config/

export CHANNEL_NAME=mychannel

setGlobalsForOrderer(){
    export CORE_PEER_LOCALMSPID=OrdererMSP
    export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/artifacts/channel/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem
    export CORE_PEER_MSPCONFIGPATH=${PWD}/artifacts/channel/crypto-config/ordererOrganizations/example.com/users/Admin@example.com/msp
    
}

setGlobalsForPeer0Org1(){
    export CORE_PEER_LOCALMSPID=Org1MSP
    export CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG1_CA
    export CORE_PEER_MSPCONFIGPATH=${PWD}/artifacts/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp
    # export CORE_PEER_MSPCONFIGPATH=${PWD}/artifacts/channel/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp
    export CORE_PEER_ADDRESS=localhost:7051
}

setGlobalsForPeer1Org1(){
    export CORE_PEER_LOCALMSPID=Org1MSP
    export CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG1_CA
    export CORE_PEER_MSPCONFIGPATH=${PWD}/artifacts/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp
    export CORE_PEER_ADDRESS=localhost:8051
    
}

setGlobalsForPeer0Org2(){
    export CORE_PEER_LOCALMSPID=Org2MSP
    export CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG2_CA
    export CORE_PEER_MSPCONFIGPATH=${PWD}/artifacts/channel/crypto-config/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp
    export CORE_PEER_ADDRESS=localhost:9051
    
}

setGlobalsForPeer1Org2(){
    export CORE_PEER_LOCALMSPID=Org2MSP
    export CORE_PEER_TLS_ROOTCERT_FILE=$PEER0_ORG2_CA
    export CORE_PEER_MSPCONFIGPATH=${PWD}/artifacts/channel/crypto-config/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp
    export CORE_PEER_ADDRESS=localhost:10051
    
}

presetup(){
    echo Vendoring Go dependencies ...
    pushd ./artifacts/src/github.com/fabcar/go
    GO111MODULE=on go mod vendor
    popd
    echo Finished vendoring Go dependencies
}
#presetup

CHANNEL_NAME="mychannel"
CC_RUNTIME_LANGUAGE="golang"
VERSION="1"
CC_SRC_PATH="./artifacts/src/github.com/fabcar/go"
CC_NAME="ehr"
SEQUENCE="1"

packageChaincode(){
    rm -rf ${CC_NAME}.tar.gz
    setGlobalsForPeer0Org1
    peer lifecycle chaincode package ${CC_NAME}.tar.gz \
    --path ${CC_SRC_PATH} --lang ${CC_RUNTIME_LANGUAGE} \
    --label ${CC_NAME}_${VERSION}
    echo "===================== Chaincode is packaged on peer0.org1 ===================== "
}

installChaincode(){
    setGlobalsForPeer0Org1
    peer lifecycle chaincode install ${CC_NAME}.tar.gz
    echo "===================== Chaincode is installed on peer0.org1 ===================== "
    
    setGlobalsForPeer1Org1
    peer lifecycle chaincode install ${CC_NAME}.tar.gz
    echo "===================== Chaincode is installed on peer1.org1 ===================== "
    
    setGlobalsForPeer0Org2
    peer lifecycle chaincode install ${CC_NAME}.tar.gz
    echo "===================== Chaincode is installed on peer0.org2 ===================== "
    
    setGlobalsForPeer1Org2
    peer lifecycle chaincode install ${CC_NAME}.tar.gz
    echo "===================== Chaincode is installed on peer1.org2 ===================== "
}

queryInstalled(){
    setGlobalsForPeer0Org1
    peer lifecycle chaincode queryinstalled >&log.txt
    cat log.txt
    PACKAGE_ID=$(sed -n "/${CC_NAME}_${VERSION}/{s/^Package ID: //; s/, Label:.*$//; p;}" log.txt)
    echo PackageID is ${PACKAGE_ID}
    echo "===================== Query installed successful on peer0.org1 on channel ===================== "
}

approveForMyOrg1(){
    setGlobalsForPeer0Org1

    peer lifecycle chaincode approveformyorg -o localhost:7050  \
    --ordererTLSHostnameOverride orderer.example.com --tls $CORE_PEER_TLS_ENABLED \
    --cafile $ORDERER_CA --channelID $CHANNEL_NAME --name ${CC_NAME} --version ${VERSION} \
    --init-required --package-id ${PACKAGE_ID} --sequence ${SEQUENCE}

    echo "===================== chaincode approved from org 1 ===================== "
    
}

# --signature-policy "OR ('Org1MSP.member')"
# --peerAddresses localhost:7051 --tlsRootCertFiles $PEER0_ORG1_CA --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA
# --peerAddresses peer0.org1.example.com:7051 --tlsRootCertFiles $PEER0_ORG1_CA --peerAddresses peer0.org2.example.com:9051 --tlsRootCertFiles $PEER0_ORG2_CA
#--channel-config-policy Channel/Application/Admins
# --signature-policy "OR ('Org1MSP.peer','Org2MSP.peer')"


checkCommitReadyness(){
    setGlobalsForPeer0Org1
    peer lifecycle chaincode checkcommitreadiness \
    --channelID $CHANNEL_NAME --name ${CC_NAME} --version ${VERSION} \
    --sequence ${SEQUENCE} --output json --init-required
    echo "===================== checking commit readyness from org 1 ===================== "
}

approveForMyOrg2(){
    setGlobalsForPeer0Org2

    peer lifecycle chaincode approveformyorg -o localhost:7050 \
    --ordererTLSHostnameOverride orderer.example.com --tls $CORE_PEER_TLS_ENABLED \
    --cafile $ORDERER_CA --channelID $CHANNEL_NAME --name ${CC_NAME} \
    --version ${VERSION} --init-required --package-id ${PACKAGE_ID} \
    --sequence ${SEQUENCE}

    echo "===================== chaincode approved from org 2 ===================== "
}

checkCommitReadyness(){
    
    setGlobalsForPeer0Org1
    peer lifecycle chaincode checkcommitreadiness --channelID $CHANNEL_NAME --peerAddresses localhost:7051 --tlsRootCertFiles $PEER0_ORG1_CA --name ${CC_NAME} --version ${VERSION} --sequence ${SEQUENCE} --output json --init-required
    echo "===================== checking commit readyness from org 1 ===================== "
}

commitChaincodeDefination(){
    setGlobalsForPeer0Org1
    #set -x  ## Turn on debugging mode
    peer lifecycle chaincode commit -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls $CORE_PEER_TLS_ENABLED  --cafile $ORDERER_CA --channelID $CHANNEL_NAME --name ${CC_NAME} --peerAddresses localhost:7051 --tlsRootCertFiles $PEER0_ORG1_CA --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA --version ${VERSION} --sequence ${SEQUENCE} --init-required
    #set +x  ## Turn off debugging mode
   
}

queryCommitted(){
    setGlobalsForPeer0Org1
    peer lifecycle chaincode querycommitted --channelID $CHANNEL_NAME --name ${CC_NAME}
    
}

chaincodeInvokeInit(){
    setGlobalsForPeer0Org1
    peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA -C $CHANNEL_NAME -n ${CC_NAME} --peerAddresses localhost:7051 --tlsRootCertFiles $PEER0_ORG1_CA --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA --isInit -c '{"Args":["initLedger"]}'

#    peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA -C $CHANNEL_NAME -n ${CC_NAME} --peerAddresses localhost:7051 --tlsRootCertFiles $PEER0_ORG1_CA --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA --isInit -c '{"Args":["createEHRRecord", "D01", "P01", "hash111","2024-09-09"]}'

}

# chaincodeInvokeInit

chaincodeInvoke(){
#     setGlobalsForPeer0Org1
#    peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com \
#    --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA -C $CHANNEL_NAME -n ${CC_NAME} \
#    --peerAddresses localhost:7051 --tlsRootCertFiles $PEER0_ORG1_CA \
#    --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA  \
#     -c '{"function": "createEHRRecord","Args": ["D02", "P01", "hash111","2024-09-09"]}'
#
#    setGlobalsForPeer0Org1
#    peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com \
#        --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA -C $CHANNEL_NAME -n ${CC_NAME} \
#        --peerAddresses localhost:7051 --tlsRootCertFiles $PEER0_ORG1_CA \
#        --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA  \
#    -c '{"function": "createEHRRecord","Args": ["D01", "P01", "hash23","2024-10-03"]}'

    setGlobalsForPeer0Org1
        peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com \
            --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA -C $CHANNEL_NAME -n ${CC_NAME} \
            --peerAddresses localhost:7051 --tlsRootCertFiles $PEER0_ORG1_CA \
            --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA  \
        -c '{"function": "activateAccess","Args": ["D01","P01","2025-04-08T12:00:00Z"]}'


    ## Create Car
    # peer chaincode invoke -o localhost:7050 \
    #     --ordererTLSHostnameOverride orderer.example.com \
    #     --tls $CORE_PEER_TLS_ENABLED \
    #     --cafile $ORDERER_CA \
    #     -C $CHANNEL_NAME -n ${CC_NAME}  \
    #     --peerAddresses localhost:7051 \
    #     --tlsRootCertFiles $PEER0_ORG1_CA \
    #     --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA $PEER_CONN_PARMS  \
    #     -c '{"function": "CreateCar","Args":["Car-ABCDEEE", "Audi", "R8", "Red", "Pavan"]}'
    
    ## Change car owner
    # peer chaincode invoke -o localhost:7050 \
    #     --ordererTLSHostnameOverride orderer.example.com \
    #     --tls $CORE_PEER_TLS_ENABLED \
    #     --cafile $ORDERER_CA \
    #     -C $CHANNEL_NAME -n ${CC_NAME}  \
    #     --peerAddresses localhost:7051 \
    #     --tlsRootCertFiles $PEER0_ORG1_CA \
    #     --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA $PEER_CONN_PARMS  \
    #     -c '{"function": "initLedger","Args":[]}'
}

chaincodeQuery(){
    setGlobalsForPeer0Org1

    # Query all cars
    # peer chaincode query -C $CHANNEL_NAME -n ${CC_NAME} -c '{"Args":["queryAllCars"]}'

    # # Query Car by Id
    # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls true --cafile $ORDERER_CA -C $CHANNEL_NAME -n ${CC_NAME} --peerAddresses localhost:7051 --tlsRootCertFiles $PEER0_ORG1_CA --peerAddresses localhost:9051 --tlsRootCertFiles $PEER0_ORG2_CA -c '{"function": "createMyAsset","Args":["ID","TEST"]}'

    # peer chaincode query -C $CHANNEL_NAME -n ${CC_NAME} -c '{"function": "createMyAsset","Args":["ID2","TEST2"]}'
    # peer chaincode query -C $CHANNEL_NAME -n ${CC_NAME} -c '{"function": "readMyAsset","Args":["ID"]}'
    # peer chaincode query -C $CHANNEL_NAME -n ${CC_NAME} -c '{"function": "createEHRRecord","Args":["ehr02", "D01", "P02", "hash222"]}'
    peer chaincode query -C $CHANNEL_NAME -n ${CC_NAME} -c '{"function": "activateAccess","Args": ["D01","P01","2025-04-08T12:00:00Z"]}'
    # peer chaincode query -C $CHANNEL_NAME -n ${CC_NAME} -c '{"Args":["createMyAsset","ID","Test"]}'
    # peer chaincode query -C $CHANNEL_NAME -n ${CC_NAME} -c '{"Args":["readMyAsset","ID"]}'
    #'{"Args":["GetSampleData","Key1"]}'
}


#presetup
# packageChaincode
 installChaincode
 queryInstalled
 approveForMyOrg1
 checkCommitReadyness
 approveForMyOrg2
 checkCommitReadyness
 commitChaincodeDefination
 queryCommitted
 sleep 3
 chaincodeInvokeInit
# sleep 5
# chaincodeInvoke
# sleep 3
# chaincodeQuery