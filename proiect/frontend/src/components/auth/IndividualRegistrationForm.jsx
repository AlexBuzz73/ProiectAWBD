import { useState } from "react";

function IndividualRegistrationForm({ onSubmit }) {
    const [formData, setFormData] = useState({
        firstName: "",
        lastName: "",
        cnp: "",
        phoneNumber: "",
        dateOfBirth: "",
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
            <h2>Individual Registration</h2>

            <div>
                <label>First Name</label>
                <input
                    type="text"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label>Last Name</label>
                <input
                    type="text"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label>CNP</label>
                <input
                    type="text"
                    name="cnp"
                    value={formData.cnp}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label>Phone Number</label>
                <input
                    type="text"
                    name="phoneNumber"
                    value={formData.phoneNumber}
                    onChange={handleChange}
                    required
                />
            </div>

            <div>
                <label>Date of Birth</label>
                <input
                    type="date"
                    name="dateOfBirth"
                    value={formData.dateOfBirth}
                    onChange={handleChange}
                    required
                />
            </div>

            <button type="submit">
                Continue
            </button>
        </form>
    );
}

export default IndividualRegistrationForm;