import { useState } from "react";

function LoginForm({ onSubmit }) {
    const [formData, setFormData] = useState({
        email: "",
        password: "",
        rememberMe: false,
    });

    const handleChange = (event) => {
        const { name, value, type, checked } = event.target;

        setFormData({
            ...formData,
            [name]: type === "checkbox" ? checked : value,
        });
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        onSubmit(formData);
    };

    return (
        <form onSubmit={handleSubmit}>
            <h2>Login</h2>

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
                    required
                />
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", margin: "0.75rem 0" }}>
                <input
                    type="checkbox"
                    id="rememberMe"
                    name="rememberMe"
                    checked={formData.rememberMe}
                    onChange={handleChange}
                />
                <label htmlFor="rememberMe" style={{ margin: 0, cursor: "pointer", fontSize: "0.875rem" }}>
                    Ține-mă minte (Remember Me)
                </label>
            </div>

            <button type="submit">
                Login
            </button>
        </form>
    );
}

export default LoginForm;