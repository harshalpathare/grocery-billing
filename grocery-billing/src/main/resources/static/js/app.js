/**
 * GROCERY BILLING — GLOBAL APP JS
 * Handles: sidebar active state, toast notifications, delete confirmations
 */




/* ====================================================
   2. TOAST NOTIFICATION SYSTEM
   Usage: showToast('Message here', 'success' | 'danger' | 'warning')
==================================================== */
(function () {
    // Create container if it doesn't exist
    if (!document.getElementById('toast-container')) {
        const container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    window.showToast = function (message, type = 'success', duration = 4000) {
        const container = document.getElementById('toast-container');

        const icons = {
            success: '<i class="bi bi-check-circle-fill text-success"></i>',
            danger:  '<i class="bi bi-exclamation-circle-fill text-danger"></i>',
            warning: '<i class="bi bi-exclamation-triangle-fill text-warning"></i>',
        };

        const toast = document.createElement('div');
        toast.className = `app-toast ${type}`;
        toast.innerHTML = `${icons[type] || icons.success} <span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(80px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 350);
        }, duration);
    };

    // Auto-show toast for Bootstrap alert messages on the page
    document.addEventListener('DOMContentLoaded', function () {
        // Convert existing success/error alerts to toast and hide the alert
        document.querySelectorAll('.alert-success[data-toast], .alert-danger[data-toast]').forEach(alert => {
            const text = alert.querySelector('span')?.textContent || alert.textContent.trim();
            const type = alert.classList.contains('alert-success') ? 'success' : 'danger';
            showToast(text, type);
            alert.style.display = 'none';
        });
    });
})();


/* ====================================================
   3. DELETE CONFIRMATION MODAL
   Usage: Add data-confirm-delete to any delete form or link
   <form data-confirm-delete data-confirm-message="Delete this product?">
==================================================== */
(function () {
    window.showDeleteConfirm = function (message, onConfirm) {
        // Remove any existing modal
        const existing = document.getElementById('deleteConfirmOverlay');
        if (existing) existing.remove();

        const overlay = document.createElement('div');
        overlay.className = 'confirm-modal-overlay';
        overlay.id = 'deleteConfirmOverlay';
        overlay.innerHTML = `
            <div class="confirm-modal-box">
                <div class="confirm-modal-icon">
                    <i class="bi bi-trash3"></i>
                </div>
                <h5 class="fw-bold mb-2" style="color:#0f172a">Are you sure?</h5>
                <p class="text-muted mb-4" style="font-size:14px">${message || 'This action cannot be undone. This record will be permanently deleted.'}</p>
                <div class="d-flex gap-3 justify-content-center">
                    <button id="cancelDelete" class="btn btn-outline-secondary px-4">Cancel</button>
                    <button id="confirmDelete" class="btn btn-danger px-4">
                        <i class="bi bi-trash3 me-1"></i> Yes, Delete
                    </button>
                </div>
            </div>`;

        document.body.appendChild(overlay);

        document.getElementById('cancelDelete').onclick = () => overlay.remove();
        overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });
        document.getElementById('confirmDelete').onclick = () => {
            overlay.remove();
            onConfirm();
        };
    };

    // Auto-attach to all forms and links with data-confirm-delete
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-confirm-delete]').forEach(el => {
            const msg = el.dataset.confirmMessage || null;

            if (el.tagName === 'FORM') {
                el.addEventListener('submit', function (e) {
                    e.preventDefault();
                    showDeleteConfirm(msg, () => el.submit());
                });
            } else if (el.tagName === 'A') {
                el.addEventListener('click', function (e) {
                    e.preventDefault();
                    const href = el.href;
                    showDeleteConfirm(msg, () => window.location.href = href);
                });
            } else if (el.tagName === 'BUTTON') {
                el.addEventListener('click', function (e) {
                    e.preventDefault();
                    const form = el.closest('form');
                    showDeleteConfirm(msg, () => { if (form) form.submit(); });
                });
            }
        });
    });
})();


/* ====================================================
   4. SIDEBAR TOGGLE
==================================================== */
document.addEventListener('DOMContentLoaded', function () {
    const toggleBtn = document.getElementById('sidebarToggle');
    if (toggleBtn) {
        toggleBtn.addEventListener('click', function () {
            document.getElementById('sidebar')?.classList.toggle('collapsed');
            document.getElementById('mainContent')?.classList.toggle('expanded');
        });
    }

    // Greeting based on time
    const greetEl = document.getElementById('greetingTime');
    if (greetEl) {
        const hour = new Date().getHours();
        greetEl.textContent = hour < 12 ? 'Morning' : hour < 17 ? 'Afternoon' : 'Evening';
    }

    // Current date
    const dateEl = document.getElementById('currentDate');
    if (dateEl) {
        dateEl.textContent = new Date().toLocaleDateString('en-IN', {
            weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
        });
    }
});

/* ====================================================
   5. AUTO-SCROLL SIDEBAR TO ACTIVE ITEM
   Waits for window load to ensure fonts and layout are finalized
==================================================== */
window.addEventListener('load', function() {
    const nav = document.querySelector('.sidebar-nav');
    const active = nav ? nav.querySelector('.nav-link.active') : null;
    if (nav && active) {
        // Scroll so the active item is near the top third of the viewport
        const target = active.offsetTop - Math.floor(nav.clientHeight / 3);
        nav.scrollTop = Math.max(0, Math.min(target, nav.scrollHeight - nav.clientHeight));
    }
});


