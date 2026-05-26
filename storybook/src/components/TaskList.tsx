import type { RootState, AppDispatch } from '../lib/store';

import Task from './Task';

import { useDispatch, useSelector } from 'react-redux';

import { updateTaskState } from '../lib/store';

export default function TaskList() {
    
    const tasks = useSelector((state: RootState) => {
        const tasksInOrder = [
            ...state.taskbox.tasks.filter((t) => t.state === 'TASK_PINNED'),
            ...state.taskbox.tasks.filter((t) => t.state !== 'TASK_PINNED'),
        ];
        const filteredTasks = tasksInOrder.filter(
            (t) => t.state === "TASK_INBOX" || t.state === 'TASK_PINNED'
        );
        return filteredTasks;
    });

    const { status } = useSelector((state: RootState) => state.taskbox);
    const dispatch = useDispatch<AppDispatch>();
    const pinTask = (value: string) => {
        dispatch(updateTaskState({ id: value, newTaskState: 'TASK_ARCHIVED' }));
    }
    const archiveTask = (value: string) => {
        dispatch(updateTaskState({ id: value, newTaskState: 'TASK_ARCHIVED' }))
    }

    const LoadingRow = (
        <div className="loading-item">
        <span className="glow-checkbox" />
        <span className="glow-text">
            <span>Loading</span> <span>cool</span> <span>state</span>
        </span>
        </div>
    );

    if (status === "loading") {
        return (
        <div className="list-items" data-testid="loading" key={"loading"}>
            {LoadingRow}
            {LoadingRow}
            {LoadingRow}
            {LoadingRow}
            {LoadingRow}
            {LoadingRow}
        </div>
        );
    }

    if (tasks.length === 0) {
        return (
        <div className="list-items" key={"empty"} data-testid="empty">
            <div className="wrapper-message">
            <span className="icon-check" />
            <p className="title-message">You have no tasks</p>
            <p className="subtitle-message">Sit back and relax</p>
            </div>
        </div>
        );
    }

    const tasksInOrder = [
        ...tasks.filter((t) => t.state === "TASK_PINNED"),
        ...tasks.filter((t) => t.state !== "TASK_PINNED"),
    ];

    return (
        <div className="list-items">
            {tasksInOrder.map((task) => (
                <Task
                  key={task.id}
                  task={task}
                  onPinTask={pinTask}
                  onArchiveTask={archiveTask}
                />
            ))}
        </div>
    )
}