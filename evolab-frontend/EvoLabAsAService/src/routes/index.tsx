import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import {
    createBrowserRouter,
    RouterProvider
} from "react-router-dom"
import { Layout } from '../components/layout/Layout'
import { Home } from '../pages/home/Home'
import {Register} from "../pages/Auth/Register.tsx";
import {Login} from "../pages/Auth/Login.tsx";
import {AuthProvider} from "../contexts/AuthContext";
import {Profile} from "../pages/profile/Profile";
import { ProtectedRoute } from './ProtectRoute.tsx'
import { Credentials } from '../pages/credentials/Credentials'
import Projects from '../pages/projects/Projects'
import ProjectDetail from '../pages/projects/ProjectDetail'
import Configs from '../pages/configs/Configs'
import {ValidCredentialsProvider} from "../contexts/ValidCredentialsContext.tsx"

const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,
    children: [
        { index: true, element: <Home /> },
        { path: "projects", element: <ProtectedRoute><Projects /></ProtectedRoute> },
        { path: "projects/:id", element: <ProtectedRoute><ProjectDetail /></ProtectedRoute> },
        { path: "credentials", element: <ProtectedRoute><Credentials /></ProtectedRoute> },
     //   { path: "contact", element: <Contact /> }, 
        { path: "config", element: <ProtectedRoute><Configs /></ProtectedRoute> },
        { path : "register", element: <Register />,},
        { path : "login", element: <Login />},
        { path : "profile", element: <ProtectedRoute> <Profile /></ProtectedRoute>},
    ]
  }
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
        <ValidCredentialsProvider>
            <RouterProvider router={router} />
        </ValidCredentialsProvider>
    </AuthProvider>
  </StrictMode>,
)
