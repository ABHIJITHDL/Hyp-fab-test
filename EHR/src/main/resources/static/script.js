document.getElementById('loginForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent the default form submission

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const mspId = document.getElementById('mspId').value;

    const loginData = {
            username: username,
            password: password,
            mspId: mspId
        };

        fetch('/fabric/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        })
    .then(response => {
        if (response.ok) {
            return response.text(); // Assuming the response contains the JWT as plain text
        } else {
            throw new Error('Login failed');
        }
    })
    .then(token => {
        localStorage.setItem('jwt', token); // Save the JWT in local storage
        redirectToHome(username,mspId); // Redirect based on mspId
    })
    .catch(error => {
        console.error('Error during login:', error);
        alert('An error occurred during login.\nPlease verify your credentials and try again.');
    });
});

function redirectToHome(username, mspId) {
    if (username === 'admin') {
        window.location.href = '/admin.html';
    }
    else if (mspId === 'Org1MSP') {
        window.location.href = '/doctor.html';
    } else if (mspId === 'Org2MSP') {
        window.location.href = '/patient.html';
    } else {
        alert('Unknown MSP ID');
    }
}

// Function to get the JWT from local storage
function getJwtFromLocalStorage() {
    return localStorage.getItem('jwt');
}

// Function to include JWT in the Authorization header for subsequent requests
function fetchWithAuth(url, options = {}) {
    const token = getJwtFromLocalStorage();
    if (!options.headers) {
        options.headers = {};
    }
    options.headers['Authorization'] = `Bearer ${token}`;
    return fetch(url, options);
}


// Example usage of fetchWithAuth
document.getElementById('fetchProtectedData').addEventListener('click', function() {
    fetchWithAuth('/api/protected')
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error('Failed to fetch protected data');
            }
        })
        .then(data => {
            console.log('Protected data:', data);
        })
        .catch(error => {
            console.error('Error fetching protected data:', error);
        });
});