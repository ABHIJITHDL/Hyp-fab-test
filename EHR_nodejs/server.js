const express= require('express')
const app=express();
const dotenv=require('dotenv').config();
const port = process.env.PORT || 5000;
const connectDb=require('./config/dbConnection.js');
const errorHandler = require("./middleware/errorHandler.js")

connectDb();
app.use(express.json())
app.use('/api/doctor',require('./routes/doctorRouter.js'))
app.use(errorHandler)

app.listen(port,()=>{
    console.log(`server started on port ${port}`)
})