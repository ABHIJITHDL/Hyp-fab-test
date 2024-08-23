const { Gateway, Wallets} = require('fabric-network');
const helper = require('./helper');

async function invokeTransaction(channelName,chaincodeName,fcn,args,username,userOrg){
    try{
        const ccp = await helper.getCCP(userOrg);
    const caURL = await helper.getCaUrl(userOrg,ccp);
    const walletPath = await helper.getWalletPathByOrg(userOrg);
    const wallet = await Wallets.newFileSystemWallet(walletPath);

    const identity = await wallet.get(username);
    if(!identity){
        console.log(`An identity for the user ${username} does not exist in the wallet`);
        return;
    }

    const gateway = new Gateway();
    await gateway.connect(ccp,{wallet,identity:username,discovery:{enabled:true,asLocalhost:true}});

    const network = await gateway.getNetwork(channelName);

    const contract = network.getContract(chaincodeName);

    let result;
    let message;
    if(fcn === "createCar"){
        result = await contract.submitTransaction(fcn,args[0], args[1], args[2], args[3], args[4]);
        message = `created car asset with key ${args[0]}`;
    }

    console.log(`Wallet path ${walletPath}`);
    }catch(err){
        console.error(`Failed to submit transaction ${err}`)
    }
}

invokeTransaction("mychannel","fabcar","createCar",["test1","AUDI111","F1-Reloaded","White","Pulkit"],"user1","org1");