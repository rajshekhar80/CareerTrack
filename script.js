
const API_URL = "http://localhost:8080/api/applications";

const form = document.getElementById("applicationForm");
const applicationList = document.getElementById("applicationList");

let applications = [];

// DASHBOARD UPDATE
function updateDashboard() {
    document.getElementById("totalCount").textContent =
        applications.length;

    document.getElementById("shortlistedCount").textContent =
        applications.filter(a => a.status === "Shortlisted").length;

    document.getElementById("interviewCount").textContent =
        applications.filter(a => a.status === "Interview").length;

    document.getElementById("selectedCount").textContent =
        applications.filter(a => a.status === "Selected").length;
}

// SAFE HTML TEXT
function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[char]);
}

// LOAD APPLICATIONS FROM JAVA
async function loadApplications() {
    try {
        const response = await fetch(API_URL);

        if (!response.ok) {
            throw new Error("Unable to load applications");
        }

        const data = await response.json();
        applications = data.applications || [];

        displayApplications();
        updateDashboard();

    } catch (error) {
        console.error("Loading error:", error);
        applicationList.innerHTML =
            `<tr><td colspan="5" class="empty">
            Cannot connect to Java server. Please start the server.
            </td></tr>`;
    }
}

// DISPLAY APPLICATIONS
function displayApplications() {
    applicationList.innerHTML = "";

    if (applications.length === 0) {
        applicationList.innerHTML = `
            <tr>
                <td colspan="5" class="empty">
                    No applications yet. Add your first internship!
                </td>
            </tr>`;
        return;
    }

    applications.forEach(app => {
        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${escapeHTML(app.company)}</td>
            <td>${escapeHTML(app.role)}</td>
            <td>${escapeHTML(app.stipend || "-")}</td>
            <td>
                <select class="status-select"
                    onchange="updateStatus(${app.id}, this.value)">
                    ${["Applied", "Shortlisted", "Interview", "Selected", "Rejected"]
                        .map(status => `
                            <option value="${status}"
                                ${app.status === status ? "selected" : ""}>
                                ${status}
                            </option>
                        `).join("")}
                </select>
            </td>
            <td>
                <button class="delete-btn"
                    onclick="deleteApplication(${app.id})">
                    Delete
                </button>
            </td>
        `;

        applicationList.appendChild(row);
    });
}

// ADD APPLICATION
form.addEventListener("submit", async function(event) {
    event.preventDefault();

    const company = document.getElementById("company").value.trim();
    const role = document.getElementById("role").value.trim();
    const status = document.getElementById("status").value;
    const stipend = document.getElementById("stipend").value.trim();

    if (!company || !role) {
        alert("Please enter company name and job role.");
        return;
    }

    const formData = new URLSearchParams();
    formData.append("company", company);
    formData.append("role", role);
    formData.append("status", status);
    formData.append("stipend", stipend);

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error("Application could not be added");
        }

        alert("Application added successfully!");

        form.reset();
        await loadApplications();

    } catch (error) {
        console.error("Add error:", error);
        alert("Java server connection failed!");
    }
});

// DELETE APPLICATION
async function deleteApplication(id) {
    if (!confirm("Are you sure you want to delete this application?")) {
        return;
    }

    try {
        const response = await fetch(
            `${API_URL}?id=${id}`,
            { method: "DELETE" }
        );

        if (!response.ok) {
            throw new Error("Delete failed");
        }

        await loadApplications();

    } catch (error) {
        console.error("Delete error:", error);
        alert("Could not delete application.");
    }
}

// UPDATE APPLICATION STATUS
async function updateStatus(id, status) {
    try {
        const response = await fetch(
            `${API_URL}?id=${id}&status=${encodeURIComponent(status)}`,
            { method: "PUT" }
        );

        if (!response.ok) {
            throw new Error("Status update failed");
        }

        await loadApplications();

    } catch (error) {
        console.error("Update error:", error);
        alert("Could not update status.");
        await loadApplications();
    }
}

// START APPLICATION
loadApplications();
