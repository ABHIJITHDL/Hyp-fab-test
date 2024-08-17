const errorHandler = (err,req,res,next) => {
    const statusCode= res.statusCode ? res.statusCode: 500;
    switch(statusCode){
        case 400:
            res.json({
                title: "Bad Request",
                message: err.message,
                stackTrach: err.stack
            });
            break;
        case 404:
            res.json({
                title: "Not Found",
                message: err.message,
                stackTrach: err.stack
            });
            break;
        default:
            res.json({
                title: "Internal Server Error",
                message: err.message,
                stackTrach: err.stack
            });
            break;
    }
    res.json({title: "Not Found",message: err.message, stackTrach: err.stack});
};

module.exports = errorHandler;