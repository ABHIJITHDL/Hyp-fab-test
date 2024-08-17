const mongoose = require('mongoose')

const connectDb = async ()=>{
    try{
        const connect = await mongoose.connect(process.env.CONNECTION_STRING);
        console.log( `MongoDb Connected: ${connect.connection.host,connect.connection.name}`);
    }catch(err){
        console.error(err)
    }
}

module.exports = connectDb;