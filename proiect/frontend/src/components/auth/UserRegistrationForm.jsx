import { useState } from "react";

function UserRegistrationForm({ onSubmit, onBack }) {
    const [formData, setFormData] = useState({
        username: "",
        email: "",
        password: "",
    });

    const handleChange = (event) => {
        const { name, value } = event.target;

        setFormData({
            ...formData,
            [name]: value,
        });
    };

    const handleSubmit = (event) => {
        event.preventDefault();

        onSubmit(formData);
    };

    return (
        <form onSubmit={handleSubmit}>
            <h2>User Credentials</h2>

            <div>
                <label>Username</label>
                <input
                    type="text"
                    name="username"
                    value={formData.username}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label>Email</label>
                <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label>Password</label>
                <input
                    type="password"
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    minLength={8}
                    required
                />
            </div>

            <button type="button" onClick={onBack}>
                Back
            </button>

            <button type="submit">
                Register
            </button>
        </form>
    );
}

export default UserRegistrationForm;