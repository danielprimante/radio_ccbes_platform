'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { getUsers, setUserRole, banUser, unbanUser, getUser, type User, createUserViaApi, updateUserViaApi, deleteUserViaApi } from '@/lib/api';
import Link from 'next/link';
import AdminGuard from '@/components/AdminGuard';
import UserModal from './UserModal'; // Ensure this matches filename

export default function UserManagement() {
    const [users, setUsers] = useState<User[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState<string | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | null>(null);


    const filteredUsers = users.filter(user =>
        user.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        user.handle.toLowerCase().includes(searchQuery.toLowerCase())
    );

    useEffect(() => {
        loadUsers();
    }, []);

    const loadUsers = async () => {
        try {
            setLoading(true);
            const usersData = await getUsers();
            setUsers(usersData);
        } catch (error) {
            console.error('Error loading users:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleToggleRole = async (userId: string, currentRole?: string) => {
        const newRole = currentRole === 'admin' ? 'user' : 'admin';
        if (!confirm(`¿Estás seguro de cambiar el rol de este usuario a ${newRole}?`)) return;

        setActionLoading(userId);
        try {
            await setUserRole(userId, newRole);
            await loadUsers();
        } catch (error) {
            console.error('Error updating role:', error);
            alert('Error al actualizar el rol');
        } finally {
            setActionLoading(null);
        }
    };

    const handleToggleBan = async (userId: string, isBanned: boolean) => {
        const action = isBanned ? 'desbloquear' : 'bloquear';
        if (!confirm(`¿Estás seguro de ${action} a este usuario?`)) return;

        setActionLoading(userId);
        try {
            if (isBanned) {
                await unbanUser(userId);
            } else {
                await banUser(userId);
            }
            await loadUsers();
        } catch (error) {
            console.error('Error updating ban status:', error);
            alert('Error al actualizar el estado de bloqueo');
        } finally {
            setActionLoading(null);
        }
    };

    const handleCreateUser = () => {
        setSelectedUser(null);
        setIsModalOpen(true);
    };

    const handleEditUser = (user: User) => {
        setSelectedUser(user);
        setIsModalOpen(true);
    };

    const handleDeleteUser = async (user: User) => {
        if (!confirm(`¿Estás seguro de eliminar permanentemente a ${user.name}? Esta acción no se puede deshacer.`)) return;

        setActionLoading(user.id);
        try {
            await deleteUserViaApi(user.id);
            await loadUsers();
        } catch (error: any) {
            console.error('Error deleting user:', error);
            alert(error.message);
        } finally {
            setActionLoading(null);
        }
    };

    const handleModalSubmit = async (data: any) => {
        try {
            if (selectedUser) {
                await updateUserViaApi({ id: selectedUser.id, ...data });
            } else {
                await createUserViaApi(data);
            }
            await loadUsers();
        } catch (error: any) {
            throw error;
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center p-20">
                <div className="text-gray-400 animate-pulse font-medium">Cargando base de usuarios...</div>
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* Search Bar & Actions */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4 mb-8">
                <div className="relative w-full sm:w-96">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                        </svg>
                    </div>
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="block w-full pl-10 pr-3 py-2.5 border border-gray-200 rounded-xl leading-5 bg-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500 sm:text-sm transition-all shadow-sm"
                        placeholder="Buscar por nombre o @handle..."
                    />
                </div>
                <button
                    onClick={handleCreateUser}
                    className="w-full sm:w-auto bg-red-600 text-white px-6 py-2.5 rounded-xl font-bold hover:bg-red-700 transition-all shadow-sm hover:shadow-md active:scale-95"
                >
                    + Nuevo Usuario
                </button>
            </div>


            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                <table className="w-full">
                    <thead className="bg-gray-50/50">
                        <tr>
                            <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Usuario</th>
                            <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Handle</th>
                            <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Rol</th>
                            <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest">Estado</th>
                            <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-widest text-right">Acciones</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-100">
                        {filteredUsers.length === 0 ? (
                            <tr>
                                <td colSpan={5} className="px-6 py-12 text-center text-gray-400 font-medium">
                                    No se encontraron resultados para "{searchQuery}"
                                </td>
                            </tr>
                        ) : (
                            filteredUsers.map((user) => (
                                <tr key={user.id} className={`${actionLoading === user.id ? 'opacity-40 pointer-events-none' : ''} hover:bg-gray-50/50 transition-colors`}>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="flex items-center">
                                            <div className="h-10 w-10 flex-shrink-0 bg-gray-100 rounded-full overflow-hidden border border-gray-100">
                                                {user.photoUrl ? (
                                                    <img className="h-full w-full object-cover" src={user.photoUrl} alt="" />
                                                ) : (
                                                    <div className="h-full w-full flex items-center justify-center text-gray-400 font-bold uppercase text-sm">
                                                        {user.name.charAt(0)}
                                                    </div>
                                                )}
                                            </div>
                                            <div className="ml-4">
                                                <div className="text-sm font-bold text-gray-900">{user.name}</div>
                                                <div className="text-xs text-gray-400">{user.email || 'Email no disponible'}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="text-sm font-medium text-red-600">@{user.handle.replace('@', '')}</div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2.5 py-1 text-[10px] font-black uppercase rounded-lg ${user.role === 'admin' ? 'bg-purple-100 text-purple-700' : 'bg-gray-100 text-gray-600'
                                            }`}>
                                            {user.role === 'admin' ? 'Administrador' : 'Usuario'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2.5 py-1 text-[10px] font-black uppercase rounded-lg ${user.isBanned ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'
                                            }`}>
                                            {user.isBanned ? 'Baneado' : 'Activo'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-bold space-x-3">
                                        <button onClick={() => handleEditUser(user)} className="text-blue-600 hover:text-blue-800 transition-colors">Editar</button>
                                        <button onClick={() => handleToggleRole(user.id, user.role)} className="text-gray-500 hover:text-purple-600 transition-colors">Rol</button>
                                        <button onClick={() => handleToggleBan(user.id, user.isBanned)} className={user.isBanned ? 'text-green-600' : 'text-orange-500'}>{user.isBanned ? 'Activar' : 'Banear'}</button>
                                        <button onClick={() => handleDeleteUser(user)} className="text-red-400 hover:text-red-600 transition-colors">Eliminar</button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            <UserModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSubmit={handleModalSubmit}
                user={selectedUser}
            />
        </div>
    );
}
