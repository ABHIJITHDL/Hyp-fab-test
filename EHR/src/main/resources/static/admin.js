document.addEventListener('DOMContentLoaded', function() {
    const jwt = localStorage.getItem('jwt'); // Retrieve the JWT from local storage
    const decodedToken = jwt_decode(jwt); // Decode the JWT
    const mspId = decodedToken.mspId; // Extract the mspId from the decoded token

    if (mspId === 'Org2MSP') {
        document.getElementById('ehrUploadContainer').style.display = 'block'; // Show the EHR upload option
    } else {
        document.getElementById('ehrUploadContainer').style.display = 'none'; // Hide the EHR upload option
    }
});

document.getElementById('registerForm').addEventListener('submit', async function(event) {
    event.preventDefault(); // Prevent the default form submission

    const formData = new FormData();
    formData.append('username', document.getElementById('username').value);
    formData.append('password', document.getElementById('password').value);

    const fileInput = document.getElementById('file');
    if (fileInput && fileInput.files.length > 0) {
        formData.append('file', fileInput.files[0]);
    }

    const jwt = localStorage.getItem('jwt'); // Retrieve the JWT from local storage

    const addButton = document.querySelector('.btn');
    if (addButton.disabled) {
        return; // Prevent multiple submissions
    }
    addButton.disabled = true; // Disable the add button
    showLoading(); // Show loading animation

    try {
        console.log('Sending request to /fabric/admin/register');
        const response = await fetch('/fabric/register', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}` // Include the JWT in the Authorization header
            },
            body: formData
        });

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const data = await response.text(); // Use response.text() to handle empty responses
        alert('User registered and  successfully!');
    } catch (error) {
        console.error('Error during registration and upload:', error);
        alert('An error occurred during registration and upload.\nPlease verify your credentials and try again.');
    } finally {
        hideLoading(); // Hide loading animation
        setTimeout(() => {
            addButton.disabled = false; // Enable the add button after a delay
        }, 3000); // Add a delay of 3 seconds before enabling the button again
    }
});

document.getElementById('logoutButton').addEventListener('click', function() {
    localStorage.removeItem('jwt'); // Remove the JWT from local storage
    window.location.href = '/login.html'; // Redirect to the login page
});

function showLoading() {
    document.getElementById('loading').style.display = 'block';
}

function hideLoading() {
    document.getElementById('loading').style.display = 'none';
}
document.getElementById('logoutButton').addEventListener('click', function() {
    localStorage.removeItem('jwt'); // Remove the JWT from local storage
    window.location.href = '/login.html'; // Redirect to the login page
});