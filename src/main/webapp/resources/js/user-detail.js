
function enableEdit() {
    document.getElementById('firstName').removeAttribute('readonly');
    document.getElementById('lastName').removeAttribute('readonly');
    document.getElementById('department').disabled = false;
    document.getElementById('status').disabled = false;

    var roleBoxes = document.querySelectorAll('.role-checkbox');
    roleBoxes.forEach(function (box) {
        box.disabled = false;
    });

    document.getElementById('editBtn').style.display = 'none';
    document.getElementById('saveBtn').style.display = 'inline-block';
    document.getElementById('cancelBtn').style.display = 'inline-block';
}

function cancelEdit() {
    location.reload();
}

function confirmSave() {
    return confirm('Are you sure you want to save these changes?');
}