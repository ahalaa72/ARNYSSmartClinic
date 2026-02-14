<%--
    Patient Self-Registration - Admin Queue View

    Displays list of pending patient registrations for staff review.
    Requires authentication and admin privileges.

    Part of the Patient Self-Registration Module
    @since 2025-12-29
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Registration Queue - OpenO EMR</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">

    <style>
        .queue-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 24px;
            border-radius: 12px;
            margin-bottom: 24px;
        }

        .queue-header h1 {
            margin: 0;
            font-size: 1.5rem;
        }

        .stats-card {
            background: white;
            border-radius: 8px;
            padding: 16px;
            text-align: center;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }

        .stats-card .number {
            font-size: 2rem;
            font-weight: bold;
            color: #333;
        }

        .stats-card .label {
            color: #6c757d;
            font-size: 0.875rem;
        }

        .registration-table {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            overflow: hidden;
        }

        .registration-table thead {
            background-color: #f8f9fa;
        }

        .registration-table th {
            font-weight: 600;
            color: #495057;
            padding: 16px;
            border-bottom: 2px solid #dee2e6;
        }

        .registration-table td {
            padding: 16px;
            vertical-align: middle;
        }

        .status-badge {
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 0.75rem;
            font-weight: 600;
            text-transform: uppercase;
        }

        .status-pending {
            background-color: #fff3cd;
            color: #856404;
        }

        .status-approved {
            background-color: #d1e7dd;
            color: #0f5132;
        }

        .status-rejected {
            background-color: #f8d7da;
            color: #721c24;
        }

        .warning-icon {
            color: #dc3545;
            margin-left: 4px;
        }

        .action-btn {
            padding: 4px 12px;
            font-size: 0.875rem;
        }

        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: #6c757d;
        }

        .empty-state i {
            font-size: 64px;
            margin-bottom: 16px;
            opacity: 0.5;
        }

        .filter-bar {
            background: white;
            padding: 16px;
            border-radius: 8px;
            margin-bottom: 24px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
    </style>
</head>
<body class="bg-light">
    <div class="container-fluid py-4">
        <!-- Header -->
        <div class="queue-header">
            <div class="row align-items-center">
                <div class="col">
                    <h1><i class="bi bi-clipboard-check me-2"></i>Registration Queue</h1>
                    <p class="mb-0 opacity-75">Review and approve patient registrations</p>
                </div>
                <div class="col-auto">
                    <a href="qrcode.jsp" class="btn btn-light">
                        <i class="bi bi-qr-code me-2"></i>View QR Code
                    </a>
                </div>
            </div>
        </div>

        <!-- Stats Row -->
        <div class="row g-3 mb-4" id="statsRow">
            <div class="col-md-3">
                <div class="stats-card">
                    <div class="number text-warning" id="pendingCount">-</div>
                    <div class="label">Pending</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stats-card">
                    <div class="number text-success" id="approvedCount">-</div>
                    <div class="label">Approved Today</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stats-card">
                    <div class="number text-danger" id="rejectedCount">-</div>
                    <div class="label">Rejected Today</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stats-card">
                    <div class="number text-info" id="totalCount">-</div>
                    <div class="label">Total This Week</div>
                </div>
            </div>
        </div>

        <!-- Filter Bar -->
        <div class="filter-bar">
            <div class="row align-items-center">
                <div class="col-md-3">
                    <select class="form-select" id="statusFilter">
                        <option value="pending" selected>Pending Only</option>
                        <option value="all">All Statuses</option>
                        <option value="approved">Approved</option>
                        <option value="rejected">Rejected</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-search"></i></span>
                        <input type="text" class="form-control" id="searchInput"
                               placeholder="Search by name, email, or HIN...">
                    </div>
                </div>
                <div class="col-md-3">
                    <input type="date" class="form-control" id="dateFilter">
                </div>
                <div class="col-md-2 text-end">
                    <button class="btn btn-outline-secondary" onclick="loadRegistrations()">
                        <i class="bi bi-arrow-clockwise me-1"></i>Refresh
                    </button>
                </div>
            </div>
        </div>

        <!-- Registrations Table -->
        <div class="registration-table">
            <table class="table table-hover mb-0">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Date of Birth</th>
                        <th>Contact</th>
                        <th>Submitted</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="registrationTableBody">
                    <tr>
                        <td colspan="7" class="text-center py-4">
                            <div class="spinner-border text-primary" role="status">
                                <span class="visually-hidden">Loading...</span>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>

            <!-- Empty State -->
            <div class="empty-state d-none" id="emptyState">
                <i class="bi bi-inbox"></i>
                <h4>No Registrations</h4>
                <p>There are no registrations matching your criteria.</p>
            </div>
        </div>
    </div>

    <!-- Quick Action Modal -->
    <div class="modal fade" id="quickActionModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="quickActionTitle">Confirm Action</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body" id="quickActionBody">
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-primary" id="quickActionConfirm">Confirm</button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        const API_BASE = '../../ws/rs/registration';
        let registrations = [];

        document.addEventListener('DOMContentLoaded', function() {
            loadRegistrations();

            // Set up filters
            document.getElementById('statusFilter').addEventListener('change', filterAndDisplay);
            document.getElementById('searchInput').addEventListener('input', filterAndDisplay);
            document.getElementById('dateFilter').addEventListener('change', filterAndDisplay);

            // Auto-refresh every 30 seconds
            setInterval(loadRegistrations, 30000);
        });

        async function loadRegistrations() {
            try {
                const response = await fetch(API_BASE + '/queue');

                if (response.status === 403) {
                    window.location.href = '../../index.jsp';
                    return;
                }

                const data = await response.json();
                registrations = data.registrations || [];

                updateStats();
                filterAndDisplay();

            } catch (error) {
                console.error('Failed to load registrations:', error);
                showError('Failed to load registrations');
            }
        }

        function updateStats() {
            const pending = registrations.filter(r => r.status === 'pending').length;
            document.getElementById('pendingCount').textContent = pending;

            // These would come from a stats endpoint in production
            document.getElementById('approvedCount').textContent = '-';
            document.getElementById('rejectedCount').textContent = '-';
            document.getElementById('totalCount').textContent = registrations.length;
        }

        function filterAndDisplay() {
            const statusFilter = document.getElementById('statusFilter').value;
            const searchTerm = document.getElementById('searchInput').value.toLowerCase();
            const dateFilter = document.getElementById('dateFilter').value;

            let filtered = registrations;

            // Status filter
            if (statusFilter !== 'all') {
                filtered = filtered.filter(r => r.status === statusFilter);
            }

            // Search filter
            if (searchTerm) {
                filtered = filtered.filter(r =>
                    (r.full_name && r.full_name.toLowerCase().includes(searchTerm)) ||
                    (r.email && r.email.toLowerCase().includes(searchTerm)) ||
                    (r.cell_phone && r.cell_phone.includes(searchTerm))
                );
            }

            // Date filter
            if (dateFilter) {
                const filterDate = new Date(dateFilter).toDateString();
                filtered = filtered.filter(r => {
                    const submittedDate = new Date(r.submitted_at).toDateString();
                    return submittedDate === filterDate;
                });
            }

            displayRegistrations(filtered);
        }

        function displayRegistrations(data) {
            const tbody = document.getElementById('registrationTableBody');
            const emptyState = document.getElementById('emptyState');

            if (data.length === 0) {
                tbody.innerHTML = '';
                emptyState.classList.remove('d-none');
                return;
            }

            emptyState.classList.add('d-none');

            tbody.innerHTML = data.map(r => `
                <tr>
                    <td><strong>#\${r.id}</strong></td>
                    <td>
                        \${escapeHtml(r.full_name || 'N/A')}
                        \${r.has_duplicate_warning ? '<i class="bi bi-exclamation-triangle warning-icon" title="Potential duplicate"></i>' : ''}
                        \${r.has_hin_error ? '<i class="bi bi-x-circle warning-icon" title="HIN validation error"></i>' : ''}
                    </td>
                    <td>\${r.date_of_birth || 'N/A'}</td>
                    <td>
                        <div><i class="bi bi-envelope me-1"></i>\${escapeHtml(r.email || 'N/A')}</div>
                        <div class="small text-muted"><i class="bi bi-phone me-1"></i>\${escapeHtml(r.cell_phone || 'N/A')}</div>
                    </td>
                    <td>\${formatDate(r.submitted_at)}</td>
                    <td><span class="status-badge status-\${r.status}">\${r.status}</span></td>
                    <td>
                        <a href="review.jsp?id=\${r.id}" class="btn btn-sm btn-outline-primary action-btn">
                            <i class="bi bi-eye me-1"></i>Review
                        </a>
                        \${r.status === 'pending' ? `
                            <button class="btn btn-sm btn-outline-success action-btn ms-1" onclick="quickApprove(\${r.id})">
                                <i class="bi bi-check"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-danger action-btn ms-1" onclick="quickReject(\${r.id})">
                                <i class="bi bi-x"></i>
                            </button>
                        ` : ''}
                    </td>
                </tr>
            `).join('');
        }

        function formatDate(dateString) {
            if (!dateString) return 'N/A';
            const date = new Date(dateString);
            return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
        }

        function escapeHtml(text) {
            if (!text) return '';
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        function quickApprove(id) {
            const reg = registrations.find(r => r.id === id);
            if (!reg) return;

            document.getElementById('quickActionTitle').textContent = 'Approve Registration';
            document.getElementById('quickActionBody').innerHTML = `
                <p>Are you sure you want to approve this registration?</p>
                <div class="alert alert-info">
                    <strong>\${escapeHtml(reg.full_name)}</strong><br>
                    DOB: \${reg.date_of_birth}<br>
                    Email: \${escapeHtml(reg.email)}
                </div>
            `;

            const confirmBtn = document.getElementById('quickActionConfirm');
            confirmBtn.className = 'btn btn-success';
            confirmBtn.textContent = 'Approve';
            confirmBtn.onclick = () => performApprove(id);

            new bootstrap.Modal(document.getElementById('quickActionModal')).show();
        }

        function quickReject(id) {
            const reg = registrations.find(r => r.id === id);
            if (!reg) return;

            document.getElementById('quickActionTitle').textContent = 'Reject Registration';
            document.getElementById('quickActionBody').innerHTML = `
                <p>Please provide a reason for rejection:</p>
                <div class="mb-3">
                    <textarea class="form-control" id="rejectionReason" rows="3"
                              placeholder="Reason for rejection..."></textarea>
                </div>
                <div class="alert alert-warning">
                    <strong>\${escapeHtml(reg.full_name)}</strong> will be notified of this rejection.
                </div>
            `;

            const confirmBtn = document.getElementById('quickActionConfirm');
            confirmBtn.className = 'btn btn-danger';
            confirmBtn.textContent = 'Reject';
            confirmBtn.onclick = () => performReject(id);

            new bootstrap.Modal(document.getElementById('quickActionModal')).show();
        }

        async function performApprove(id) {
            try {
                const response = await fetch(API_BASE + '/queue/' + id + '/approve', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({})
                });

                const result = await response.json();

                if (result.success) {
                    bootstrap.Modal.getInstance(document.getElementById('quickActionModal')).hide();
                    loadRegistrations();
                    showSuccess('Registration approved. Demographic #' + result.demographic_no + ' created.');
                } else {
                    alert(result.error || 'Failed to approve');
                }
            } catch (error) {
                console.error('Approve failed:', error);
                alert('Failed to approve registration');
            }
        }

        async function performReject(id) {
            const reason = document.getElementById('rejectionReason').value;

            if (!reason.trim()) {
                alert('Please provide a rejection reason');
                return;
            }

            try {
                const response = await fetch(API_BASE + '/queue/' + id + '/reject', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ reason: reason })
                });

                const result = await response.json();

                if (result.success) {
                    bootstrap.Modal.getInstance(document.getElementById('quickActionModal')).hide();
                    loadRegistrations();
                    showSuccess('Registration rejected.');
                } else {
                    alert(result.error || 'Failed to reject');
                }
            } catch (error) {
                console.error('Reject failed:', error);
                alert('Failed to reject registration');
            }
        }

        function showSuccess(message) {
            // Simple toast notification
            const toast = document.createElement('div');
            toast.className = 'position-fixed top-0 end-0 p-3';
            toast.style.zIndex = '9999';
            toast.innerHTML = `
                <div class="toast show bg-success text-white">
                    <div class="toast-body">
                        <i class="bi bi-check-circle me-2"></i>\${message}
                    </div>
                </div>
            `;
            document.body.appendChild(toast);
            setTimeout(() => toast.remove(), 3000);
        }

        function showError(message) {
            const toast = document.createElement('div');
            toast.className = 'position-fixed top-0 end-0 p-3';
            toast.style.zIndex = '9999';
            toast.innerHTML = `
                <div class="toast show bg-danger text-white">
                    <div class="toast-body">
                        <i class="bi bi-x-circle me-2"></i>\${message}
                    </div>
                </div>
            `;
            document.body.appendChild(toast);
            setTimeout(() => toast.remove(), 3000);
        }
    </script>
</body>
</html>
