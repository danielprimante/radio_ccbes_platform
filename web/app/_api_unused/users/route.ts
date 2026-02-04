import { NextRequest, NextResponse } from 'next/server';
import { adminAuth, adminDb } from '@/lib/firebase-admin';

// Helper to check if caller is an admin (simplified for now, ideally verify ID token)
const checkAdmin = async (req: NextRequest) => {
    // In a real app, send ID token in header, verify it with adminAuth.verifyIdToken, 
    // and check custom claims or db role.
    // For this MVP, we might trust the client-side guard + Firestore rules, 
    // BUT API routes are public endpoints. We MUST verify auth.

    const authHeader = req.headers.get('Authorization');
    if (!authHeader?.startsWith('Bearer ')) {
        return null;
    }

    const token = authHeader.split('Bearer ')[1];
    try {
        const decodedToken = await adminAuth.verifyIdToken(token);
        // Check if user is admin in Firestore
        const userDoc = await adminDb.collection('users').doc(decodedToken.uid).get();
        const userData = userDoc.data();
        if (userData?.role === 'admin') {
            return decodedToken;
        }
        return null;
    } catch (error) {
        console.error('Verify token error:', error);
        return null;
    }
};

export async function POST(req: NextRequest) {
    // Verify Admin
    const admin = await checkAdmin(req);
    if (!admin) {
        return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    try {
        const body = await req.json();
        const { email, password, name, handle, bio, role = 'user' } = body;

        // 1. Create Auth User
        const userRecord = await adminAuth.createUser({
            email,
            password,
            displayName: name,
        });

        // 2. Create Firestore Document
        await adminDb.collection('users').doc(userRecord.uid).set({
            name,
            handle,
            bio: bio || '',
            email,
            role,
            isBanned: false,
            createdAt: new Date().toISOString(),
            photoUrl: userRecord.photoURL || null
        });

        return NextResponse.json({ id: userRecord.uid, message: 'User created' });
    } catch (error: any) {
        console.error('Create user error:', error);
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}

export async function PUT(req: NextRequest) {
    const admin = await checkAdmin(req);
    if (!admin) {
        return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    try {
        const body = await req.json();
        const { id, name, handle, bio, role, password } = body;

        if (!id) return NextResponse.json({ error: 'ID required' }, { status: 400 });

        // 1. Update Auth User (if needed)
        const updateAuth: any = {};
        if (name) updateAuth.displayName = name;
        if (password) updateAuth.password = password;

        if (Object.keys(updateAuth).length > 0) {
            await adminAuth.updateUser(id, updateAuth);
        }

        // 2. Update Firestore
        const updateData: any = {};
        if (name) updateData.name = name;
        if (handle) updateData.handle = handle;
        if (bio !== undefined) updateData.bio = bio;
        if (role) updateData.role = role;

        if (Object.keys(updateData).length > 0) {
            await adminDb.collection('users').doc(id).update(updateData);
        }

        return NextResponse.json({ message: 'User updated' });
    } catch (error: any) {
        console.error('Update user error:', error);
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}

export async function DELETE(req: NextRequest) {
    const admin = await checkAdmin(req);
    if (!admin) {
        return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    try {
        const { searchParams } = new URL(req.url);
        const id = searchParams.get('id');

        if (!id) return NextResponse.json({ error: 'ID required' }, { status: 400 });

        // 1. Delete Auth User
        await adminAuth.deleteUser(id);

        // 2. Delete Firestore Document
        await adminDb.collection('users').doc(id).delete();

        return NextResponse.json({ message: 'User deleted' });

    } catch (error: any) {
        console.error('Delete user error:', error);
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
