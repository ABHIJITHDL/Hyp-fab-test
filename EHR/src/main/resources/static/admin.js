document.getElementById('registerForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent the default form submission

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    const registrationData = new URLSearchParams();
    registrationData.append('username', username);
    registrationData.append('password', password);

    const jwt = localStorage.getItem('jwt'); // Retrieve the JWT from local storage

    fetch('/fabric/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Authorization': `Bearer ${jwt}` // Include the JWT in the Authorization header
        },
        body: registrationData.toString()
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.text(); // Use response.text() to handle empty responses
    })
    .then(token => {
        localStorage.setItem('jwt', token); // Save the JWT in local storage
        alert('Registration successful!');
    })
    .catch(error => {
        console.error('Error during registration:', error);
        alert('An error occurred during regisration.\nPlease verify your credentials and try again.');
        window.location.href = '/login.html'; // Redirect to the login page
    });
});

document.getElementById('logoutButton').addEventListener('click', function() {
    localStorage.removeItem('jwt'); // Remove the JWT from local storage
    window.location.href = '/login.html'; // Redirect to the login page
});