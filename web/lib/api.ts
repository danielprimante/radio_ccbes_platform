import {
    collection,
    addDoc,
    updateDoc,
    deleteDoc,
    doc,
    getDocs,
    query,
    orderBy,
    where,
    Timestamp,
    getDoc,
    setDoc
} from 'firebase/firestore';
import { db, auth } from './firebase';

export interface Post {
    id?: string;
    userId: string;
    userName: string;
    userHandle: string;
    userPhotoUrl?: string;
    content: string;
    imageUrl?: string;
    likes: number;
    comments: number;
    timestamp: Timestamp;
    category: 'all' | 'trending' | 'news' | 'reflections';
}

const postsCollection = collection(db, 'posts');

export async function getPosts(): Promise<Post[]> {
    const q = query(postsCollection, orderBy('timestamp', 'desc'));
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
    } as Post));
}

export async function getPostsByCategory(category: string): Promise<Post[]> {
    if (category === 'all') {
        return getPosts();
    }

    const q = query(
        postsCollection,
        where('category', '==', category),
        orderBy('timestamp', 'desc')
    );
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
    } as Post));
}

export async function getPost(id: string): Promise<Post | null> {
    const docRef = doc(db, 'posts', id);
    const docSnap = await getDoc(docRef);

    if (docSnap.exists()) {
        return {
            id: docSnap.id,
            ...docSnap.data()
        } as Post;
    }
    return null;
}

export async function createPost(post: Omit<Post, 'id'>): Promise<string> {
    const docRef = await addDoc(postsCollection, post);
    return docRef.id;
}

export async function updatePost(id: string, data: Partial<Post>): Promise<void> {
    const docRef = doc(db, 'posts', id);
    await updateDoc(docRef, data);
}

export async function deletePost(id: string): Promise<void> {
    const docRef = doc(db, 'posts', id);
    await deleteDoc(docRef);
}

export async function getStats() {
    const posts = await getPosts();
    const totalPosts = posts.length;
    const totalLikes = posts.reduce((sum, post) => sum + post.likes, 0);
    const totalComments = posts.reduce((sum, post) => sum + post.comments, 0);

    const categoryCount = {
        all: posts.length,
        trending: posts.filter(p => p.category === 'trending').length,
        news: posts.filter(p => p.category === 'news').length,
        reflections: posts.filter(p => p.category === 'reflections').length
    };

    return {
        totalPosts,
        totalLikes,
        totalComments,
        categoryCount
    };
}

// Likes functions (existing)
export interface Like {
    id?: string;
    postId: string;
    userId: string;
    timestamp: Timestamp;
}

const likesCollection = collection(db, 'likes');

export async function getLikesForPost(postId: string): Promise<Like[]> {
    const q = query(
        likesCollection,
        where('postId', '==', postId),
        orderBy('timestamp', 'desc')
    );
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
    } as Like));
}

export async function getLikeCount(postId: string): Promise<number> {
    const likes = await getLikesForPost(postId);
    return likes.length;
}

// User functions
export interface User {
    id: string;
    name: string;
    handle: string;
    photoUrl?: string;
    bio: string;
    isBanned: boolean;
    fcmToken?: string;
    role?: 'admin' | 'user';
    city?: string;
    phone?: string;
    email?: string;
    pronouns?: string;
    gender?: string;
    link?: string;
    category?: string;
}

const usersCollection = collection(db, 'users');

export async function getUsers(): Promise<User[]> {
    const q = query(usersCollection, orderBy('name', 'asc'));
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
    } as User));
}

export async function getUser(id: string): Promise<User | null> {
    const docRef = doc(db, 'users', id);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        return { id: docSnap.id, ...docSnap.data() } as User;
    }
    return null;
}

export async function updateUser(id: string, data: Partial<User>): Promise<void> {
    const docRef = doc(db, 'users', id);
    await updateDoc(docRef, data);

    // Cascada: si el perfil cambia (nombre o foto), actualizamos sus posts y comentarios
    await cascadeUserUpdate(id, data);
}

async function cascadeUserUpdate(userId: string, data: Partial<User>) {
    const updates: any = {};
    if (data.name) updates.userName = data.name;
    if (data.photoUrl) updates.userPhotoUrl = data.photoUrl;
    if (data.handle) updates.userHandle = data.handle;

    if (Object.keys(updates).length === 0) return;

    try {
        // Update Posts
        const postsQuery = query(postsCollection, where('userId', '==', userId));
        const postsSnap = await getDocs(postsQuery);
        const postPromises = postsSnap.docs.map(d => updateDoc(d.ref, updates));

        // Update Comments
        const commentsQuery = query(commentsCollection, where('userId', '==', userId));
        const commentsSnap = await getDocs(commentsQuery);
        const commentPromises = commentsSnap.docs.map(d => updateDoc(d.ref, updates));

        // Update Notifications
        const notifUpdates: any = {};
        if (data.name) notifUpdates.fromUserName = data.name;
        if (data.photoUrl) notifUpdates.fromUserProfilePic = data.photoUrl;

        let notifPromises: Promise<any>[] = [];
        if (Object.keys(notifUpdates).length > 0) {
            const notifQuery = query(collection(db, 'notifications'), where('fromUserId', '==', userId));
            const notifSnap = await getDocs(notifQuery);
            notifPromises = notifSnap.docs.map(d => updateDoc(d.ref, notifUpdates));
        }

        await Promise.all([...postPromises, ...commentPromises, ...notifPromises]);
    } catch (error) {
        console.error('Error cascading user update:', error);
    }
}

export async function setUserRole(id: string, role: 'admin' | 'user'): Promise<void> {
    await updateUser(id, { role });
}

export async function banUser(id: string): Promise<void> {
    await updateUser(id, { isBanned: true });
}

export async function unbanUser(id: string): Promise<void> {
    await updateUser(id, { isBanned: false });
}

// Admin API wrapper
// Admin API wrapper replacements for Static Export

export async function createUserViaApi(data: any) {
    // Static export cannot create Auth users securely.
    // We throw a specific error to catch in UI and show instructions.
    throw new Error('STATIC_EXPORT_LIMITATION');
}

export async function updateUserViaApi(data: any) {
    // Update user profile in Firestore
    const { id, ...updateData } = data;
    await updateUser(id, updateData);
    return { success: true };
}

export async function deleteUserViaApi(userId: string) {
    // Soft delete / Firestore delete only
    // We cannot delete from Auth without backend
    const docRef = doc(db, 'users', userId);
    await deleteDoc(docRef);

    // Also cascade delete posts/comments if needed, or leave them orphan?
    // For now just deleting the user profile user doc.
    return { success: true };
}


// Comments functions
export interface Comment {
    id?: string;
    postId: string;
    userId: string;
    userName: string;
    userPhotoUrl?: string;
    content: string;
    imageUrl?: string;
    likesCount?: number;
    timestamp: Timestamp;
}

const commentsCollection = collection(db, 'comments');

export async function getCommentsForPost(postId: string): Promise<Comment[]> {
    const q = query(
        commentsCollection,
        where('postId', '==', postId),
        orderBy('timestamp', 'asc')
    );
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
    } as Comment));
}

export async function deleteComment(commentId: string): Promise<void> {
    const docRef = doc(db, 'comments', commentId);
    await deleteDoc(docRef);
}

export async function updateComment(id: string, data: Partial<Comment>): Promise<void> {
    const docRef = doc(db, 'comments', id);
    await updateDoc(docRef, data);
}


// Reports & Moderation functions
export interface Report {
    id?: string;
    postId?: string;
    commentId?: string;
    reportedBy: string;
    reason: string;
    status: 'pending' | 'reviewed' | 'dismissed';
    timestamp: Timestamp;
}

const reportsCollection = collection(db, 'reports');

export async function getReports(): Promise<Report[]> {
    const q = query(reportsCollection, orderBy('timestamp', 'desc'));
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
    } as Report));
}

export async function createReport(report: Omit<Report, 'id'>): Promise<string> {
    const docRef = await addDoc(reportsCollection, report);
    return docRef.id;
}

export async function updateReportStatus(id: string, status: Report['status']): Promise<void> {
    const docRef = doc(db, 'reports', id);
    await updateDoc(docRef, { status });
}



// Radio Settings functions
export interface RadioSettings {
    streamUrl: string;
    title?: string;
    subtitle?: string;
}

export interface AboutSettings {
    logoUrl: string;
    churchName: string;
    subText: string;
    location: string;
    email: string;
    phone: string;
    facebookUrl: string;
    instagramUrl: string;
    youtubeUrl: string;
}

export async function getRadioSettings(): Promise<RadioSettings | null> {
    const docRef = doc(db, 'settings', 'radio');
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        return docSnap.data() as RadioSettings;
    }
    return null;
}

export async function getAboutSettings(): Promise<AboutSettings | null> {
    const docRef = doc(db, 'settings', 'about');
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        return docSnap.data() as AboutSettings;
    }
    return null;
}

export async function updateRadioSettings(settings: RadioSettings): Promise<void> {
    const docRef = doc(db, 'settings', 'radio');
    await setDoc(docRef, settings, { merge: true });
}

export async function updateAboutSettings(settings: AboutSettings): Promise<void> {
    const docRef = doc(db, 'settings', 'about');
    await setDoc(docRef, settings, { merge: true });
}
