const mongoose = require('mongoose')

const doctorSchema = mongoose.Schema({
    name:{
        type:String,
        required:[true,"Please add the user name"],
    },
    email:{
        type:String,
        required:[true,"Please add the email addres"],
        unique:[true,"Email address already taken"]
    },
    password:{
        type:String,
        required:[true,"Please add the password"],
    },
},{
    timestamps:true,
})

module.exports = mongoose.model("Doctor",doctorSchema);